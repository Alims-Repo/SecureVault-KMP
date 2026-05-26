import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.binaryCompatibilityValidator)
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    // Force every public declaration to carry an explicit visibility modifier.
    explicitApi = ExplicitApiMode.Strict

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }

    android {
        namespace = "io.github.alimsrepo.secure.vault"
        compileSdk { version = release(37) }
        minSdk = 24

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }

    val xcfName = "SecureVaultKit"
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.security.crypto)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true,
        ),
    )

    coordinates(
        groupId = providers.gradleProperty("GROUP").get(),
        artifactId = "secure-vault",
        version = providers.gradleProperty("VERSION_NAME").get(),
    )

    pom {
        name.set("SecureVault KMP")
        description.set(
            "A small, coroutine-first Kotlin Multiplatform library for storing " +
                "secrets on Android (EncryptedSharedPreferences) and iOS (Keychain).",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/Alims-Repo/SecureVault-KMP")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("alimsrepo")
                name.set("Alim Sourav")
                email.set("sourav.0.alim@gmail.com")
                url.set("https://github.com/Alims-Repo")
            }
        }

        scm {
            url.set("https://github.com/Alims-Repo/SecureVault-KMP")
            connection.set("scm:git:git://github.com/Alims-Repo/SecureVault-KMP.git")
            developerConnection.set("scm:git:ssh://git@github.com/Alims-Repo/SecureVault-KMP.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/Alims-Repo/SecureVault-KMP/issues")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()
}

// Binary-compatibility validator pins the public ABI under /api.
// Run `./gradlew :secure-vault:apiDump` after intentional API changes.
