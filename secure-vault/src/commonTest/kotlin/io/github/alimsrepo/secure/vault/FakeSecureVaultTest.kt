/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

/** Runs the shared [SecureVaultContractTest] against the in-memory fake. */
internal class FakeSecureVaultTest : SecureVaultContractTest() {
    override suspend fun vault(): SecureVault = FakeSecureVault()
}

