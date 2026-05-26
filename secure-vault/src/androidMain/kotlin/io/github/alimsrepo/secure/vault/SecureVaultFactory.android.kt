/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import android.content.Context

/**
 * Android implementation. Hold on to the application [Context] only — never the
 * supplied one — so factories created inside an Activity or Fragment do not
 * leak it.
 */
public actual class SecureVaultFactory(context: Context) {

    private val appContext: Context = context.applicationContext

    public actual fun create(config: VaultConfig): SecureVault =
        AndroidSecureVault(appContext, config)
}

