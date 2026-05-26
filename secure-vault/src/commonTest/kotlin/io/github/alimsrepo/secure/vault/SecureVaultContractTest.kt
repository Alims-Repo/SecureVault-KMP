/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavioural contract every [SecureVault] implementation must satisfy.
 *
 * Subclasses (or the in-memory [FakeSecureVault]) supply a fresh instance via
 * [vault]. Platform-specific tests will eventually extend this class to verify
 * the real backends without rewriting the assertions.
 */
internal abstract class SecureVaultContractTest {

    protected abstract suspend fun vault(): SecureVault

    @Test
    fun put_then_get_roundtrips() = runTest {
        val v = vault()
        v.put("token", "s3cret")
        assertEquals("s3cret", v.get("token"))
    }

    @Test
    fun get_returns_null_for_unknown_key() = runTest {
        assertNull(vault().get("missing"))
    }

    @Test
    fun put_overwrites_existing_value() = runTest {
        val v = vault()
        v.put("k", "v1")
        v.put("k", "v2")
        assertEquals("v2", v.get("k"))
    }

    @Test
    fun contains_reflects_membership() = runTest {
        val v = vault()
        assertFalse(v.contains("k"))
        v.put("k", "v")
        assertTrue(v.contains("k"))
    }

    @Test
    fun remove_is_idempotent() = runTest {
        val v = vault()
        v.put("k", "v")
        v.remove("k")
        v.remove("k") // must not throw
        assertNull(v.get("k"))
    }

    @Test
    fun keys_returns_current_snapshot() = runTest {
        val v = vault()
        v.put("a", "1")
        v.put("b", "2")
        assertEquals(setOf("a", "b"), v.keys())
        v.remove("a")
        assertEquals(setOf("b"), v.keys())
    }

    @Test
    fun clear_empties_the_namespace() = runTest {
        val v = vault()
        v.put("a", "1")
        v.put("b", "2")
        v.clear()
        assertTrue(v.keys().isEmpty())
    }

    @Test
    fun blank_key_is_rejected_on_every_operation() = runTest {
        val v = vault()
        assertFailsWith<VaultException.InvalidKey> { v.put(" ", "x") }
        assertFailsWith<VaultException.InvalidKey> { v.get("") }
        assertFailsWith<VaultException.InvalidKey> { v.contains("") }
        assertFailsWith<VaultException.InvalidKey> { v.remove("") }
    }

    // ------------------------------------------------------------------
    // Observability (since 0.3.0)
    // ------------------------------------------------------------------

    @Test
    fun observe_emits_current_value_immediately() = runTest {
        val v = vault()
        v.put("k", "v0")
        assertEquals("v0", v.observe("k").first())
    }

    @Test
    fun observe_emits_null_for_unknown_key() = runTest {
        assertNull(vault().observe("missing").first())
    }

    /**
     * The reactivity contract: a write performed *after* an observer has
     * subscribed must be visible to that observer without an explicit re-read.
     */
    @Test
    fun observer_sees_writes_without_explicit_reread() = runTest {
        val v = vault()
        v.put("k", "v0")

        val collector = async {
            v.observe("k").take(3).toList()
        }
        // Give the observer a chance to subscribe and emit the initial value.
        // runTest's virtual clock advances eagerly between coroutines, so a
        // single yield via put() is enough.
        v.put("k", "v1")
        v.put("k", "v2")

        assertEquals(listOf("v0", "v1", "v2"), collector.await())
    }

    @Test
    fun observe_emits_null_after_remove() = runTest {
        val v = vault()
        v.put("k", "v")
        val collector = async { v.observe("k").take(2).toList() }
        v.remove("k")
        assertEquals(listOf("v", null), collector.await())
    }

    @Test
    fun observe_emits_null_after_clear() = runTest {
        val v = vault()
        v.put("k", "v")
        val collector = async { v.observe("k").take(2).toList() }
        v.clear()
        assertEquals(listOf("v", null), collector.await())
    }

    @Test
    fun observe_keys_reflects_mutations() = runTest {
        val v = vault()
        val collector = async { v.observeKeys().take(3).toList() }
        v.put("a", "1")
        v.put("b", "2")
        assertEquals(
            listOf(emptySet(), setOf("a"), setOf("a", "b")),
            collector.await(),
        )
    }

    @Test
    fun observe_rejects_blank_key_on_collect() = runTest {
        assertFailsWith<VaultException.InvalidKey> {
            vault().observe("").first()
        }
    }
}
