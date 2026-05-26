import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.binaryCompatibilityValidator)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }

    android {
        namespace = "io.github.alimsrepo.secure.vault.compose"
        compileSdk { version = release(37) }
        minSdk = 24

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }

    val xcfName = "SecureVaultComposeKit"
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":secure-vault"))
            implementation(compose.runtime)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
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

    // Coordinates come from secure-vault-compose/gradle.properties so the AAR
    // publication race (groupId$plugin finalised early by AGP 9) does not bite
    // us a second time.

    pom {
        name.set("SecureVault KMP — Compose")
        description.set(
            "Compose Multiplatform integration for SecureVault KMP: rememberSecureVault, " +
                "VaultState lifecycle, and a LocalSecureVault CompositionLocal.",
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
                id.set("alims-repo")
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

