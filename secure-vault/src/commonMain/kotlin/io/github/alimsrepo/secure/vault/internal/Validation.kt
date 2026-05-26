/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.internal

import io.github.alimsrepo.secure.vault.VaultException

/**
 * Validates a user-supplied entry key. Centralised so every backend enforces
 * the same contract.
 *
 * @throws VaultException.InvalidKey if [key] is blank.
 */
internal fun requireValidKey(key: String) {
    if (key.isBlank()) throw VaultException.InvalidKey(key)
}

