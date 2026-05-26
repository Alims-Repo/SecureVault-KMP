plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLint) apply false

    // Apply Dokka at the root so we can aggregate documentation from every
    // published module into a single HTML site under docs/api/. The same
    // plugin is applied independently by :secure-vault and :secure-vault-compose
    // to drive their javadoc-jar publication; this root application is purely
    // for the multi-module landing page.
    alias(libs.plugins.dokka)
}

dependencies {
    // Aggregation roots — every published artifact should be listed here.
    dokka(project(":secure-vault"))
    dokka(project(":secure-vault-compose"))
}

dokka {
    moduleName.set("SecureVault KMP")

    pluginsConfiguration.html {
        footerMessage.set("© 2026 Alim Sourav · Apache-2.0")
    }

    dokkaPublications.html {
        // Emit straight into docs/api/ so GitHub Pages (which serves /docs)
        // exposes the API reference at /SecureVault-KMP/api/.
        outputDirectory.set(rootDir.resolve("docs/api"))
    }
}
