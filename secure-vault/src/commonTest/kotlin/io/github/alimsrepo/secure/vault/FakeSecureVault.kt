/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.alimsrepo.secure.vault.internal.requireValidKey

/**
 * In-memory [SecureVault] used only by the test source set. Kept here (not in
 * commonMain) so it stays out of the published artifact's API surface.
 *
 * The behaviour mirrors [AndroidSecureVault] / [IosSecureVault]: blank keys
 * throw [VaultException.InvalidKey]; absent reads return `null`; `clear()`
 * empties only this instance's namespace.
 */
internal class FakeSecureVault : SecureVault {

    private val mutex = Mutex()
    private val data = mutableMapOf<String, String>()

    override suspend fun put(key: String, value: String) {
        requireValidKey(key)
        mutex.withLock { data[key] = value }
    }

    override suspend fun get(key: String): String? {
        requireValidKey(key)
        return mutex.withLock { data[key] }
    }

    override suspend fun remove(key: String) {
        requireValidKey(key)
        mutex.withLock { data.remove(key) }
    }

    override suspend fun contains(key: String): Boolean {
        requireValidKey(key)
        return mutex.withLock { key in data }
    }

    override suspend fun clear() {
        mutex.withLock { data.clear() }
    }

    override suspend fun keys(): Set<String> = mutex.withLock { data.keys.toSet() }
}

