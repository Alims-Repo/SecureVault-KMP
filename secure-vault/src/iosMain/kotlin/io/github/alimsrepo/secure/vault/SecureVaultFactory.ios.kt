/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

/**
 * iOS implementation. The Keychain is a process-wide singleton, so no
 * platform handle is required at construction time.
 */
public actual class SecureVaultFactory public constructor() {

    public actual fun create(config: VaultConfig): SecureVault =
        IosSecureVault(config)
}

