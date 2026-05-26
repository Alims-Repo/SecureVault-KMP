/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import io.github.alimsrepo.secure.vault.internal.requireValidKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [SecureVault] used only by the test source set. Kept here (not in
 * commonMain) so it stays out of the published artifact's API surface.
 *
 * The behaviour mirrors [AndroidSecureVault] / [IosSecureVault]: blank keys
 * throw [VaultException.InvalidKey]; absent reads return `null`; `clear()`
 * empties only this instance's namespace; [observe] / [observeKeys] re-emit
 * after every mutation made on this instance.
 */
internal class FakeSecureVault : SecureVault {

    private val mutex = Mutex()
    private val data = mutableMapOf<String, String>()

    /** Fires once after every successful mutation. extraBufferCapacity = 1
     *  guarantees emissions are non-suspending. */
    private val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun put(key: String, value: String) {
        requireValidKey(key)
        mutex.withLock { data[key] = value }
        invalidations.tryEmit(Unit)
    }

    override suspend fun get(key: String): String? {
        requireValidKey(key)
        return mutex.withLock { data[key] }
    }

    override suspend fun remove(key: String) {
        requireValidKey(key)
        mutex.withLock { data.remove(key) }
        invalidations.tryEmit(Unit)
    }

    override suspend fun contains(key: String): Boolean {
        requireValidKey(key)
        return mutex.withLock { key in data }
    }

    override suspend fun clear() {
        mutex.withLock { data.clear() }
        invalidations.tryEmit(Unit)
    }

    override suspend fun keys(): Set<String> = mutex.withLock { data.keys.toSet() }

    override fun observe(key: String): Flow<String?> = flow {
        requireValidKey(key)
        emit(get(key))
        invalidations.collect { emit(get(key)) }
    }.distinctUntilChanged()

    override fun observeKeys(): Flow<Set<String>> = flow {
        emit(keys())
        invalidations.collect { emit(keys()) }
    }.distinctUntilChanged()
}
