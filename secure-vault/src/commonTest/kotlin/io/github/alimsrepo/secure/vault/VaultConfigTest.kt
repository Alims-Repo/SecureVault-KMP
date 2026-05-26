/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class VaultConfigTest {

    @Test
    fun blank_namespace_is_rejected() {
        assertFailsWith<IllegalArgumentException> { VaultConfig(namespace = "") }
        assertFailsWith<IllegalArgumentException> { VaultConfig(namespace = "   ") }
    }

    @Test
    fun namespace_with_illegal_chars_is_rejected() {
        assertFailsWith<IllegalArgumentException> { VaultConfig(namespace = "a/b") }
        assertFailsWith<IllegalArgumentException> { VaultConfig(namespace = "café") }
    }

    @Test
    fun valid_namespaces_are_accepted() {
        VaultConfig("com.acme.auth")
        VaultConfig("a")
        VaultConfig("a_b-c.0")
    }

    @Test
    fun default_accessibility_is_after_first_unlock() {
        assertEquals(Accessibility.AfterFirstUnlock, VaultConfig("ns").accessibility)
    }
}

