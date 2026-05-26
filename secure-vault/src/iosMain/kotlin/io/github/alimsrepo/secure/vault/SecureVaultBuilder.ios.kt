/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

/**
 * iOS `actual` for the top-level [SecureVault] factory. The Keychain is a
 * process-wide singleton, so no platform handle is required.
 */
public actual fun SecureVault(config: VaultConfig): SecureVault = IosSecureVault(config)

