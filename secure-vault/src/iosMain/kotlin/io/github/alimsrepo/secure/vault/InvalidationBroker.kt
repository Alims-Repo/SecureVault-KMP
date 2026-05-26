/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

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
 */
internal object InvalidationBroker {

    private val brokers = mutableMapOf<String, MutableSharedFlow<Unit>>()
    private val lock = Any()

    fun forService(service: String): MutableSharedFlow<Unit> = synchronized(lock) {
        brokers.getOrPut(service) {
            // extraBufferCapacity = 1 → tryEmit never drops and never suspends
            // even when there are no active collectors.
            MutableSharedFlow(extraBufferCapacity = 1)
        }
    }
}

