/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import kotlin.concurrent.AtomicReference
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Process-wide registry of invalidation channels, one per Keychain
 * `kSecAttrService` (== vault namespace).
 *
 * Two [IosSecureVault] instances constructed with the same namespace must see
 * each other's mutations through their respective [SecureVault.observe] flows.
 * The Keychain itself provides no in-process change notification, so we route
 * those signals through a shared [MutableSharedFlow] looked up by service name.
 *
 * Out of scope: changes performed from another process, by another framework,
 * or via raw `SecItem*` calls outside this library. Those will not be observed.
 *
 * The registry is updated through a lock-free CAS loop over an immutable map —
 * `kotlin.synchronized { }` is JVM-only and not available on Kotlin/Native.
 * On a rare race two callers may each construct a [MutableSharedFlow]; only
 * the winner is published, the loser becomes garbage. Once an entry is
 * installed, every subsequent caller for that service sees the same flow.
 */
internal object InvalidationBroker {

    private val brokers: AtomicReference<Map<String, MutableSharedFlow<Unit>>> =
        AtomicReference(emptyMap())

    fun forService(service: String): MutableSharedFlow<Unit> {
        while (true) {
            val current = brokers.value
            current[service]?.let { return it }
            // extraBufferCapacity = 1 → tryEmit never drops and never suspends
            // even when there are no active collectors.
            val created = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val updated = current + (service to created)
            if (brokers.compareAndSet(current, updated)) return created
            // Lost the race — re-read and try again (or return the winner).
        }
    }
}

