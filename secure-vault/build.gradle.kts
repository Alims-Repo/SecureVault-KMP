import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)

    id("com.vanniktech.maven.publish") version "0.30.0"
    id("signing")
}

kotlin {

    android {
        namespace = "io.github.alimsrepo.secure.vault"
        compileSdk {
            version = release(37)
        }
        minSdk = 24
    }

    val xcfName = "secure-vaultKit"
    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

            }
        }

        androidMain {
            dependencies {

            }
        }

        iosMain {
            dependencies {

            }
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.alims-repo",
        artifactId = "secure-vault",
        version = "1.0.0"
    )

    pom {
        name.set("Secure Vault KMP")
        description.set("")
        inceptionYear.set("2026")
        url.set("https://github.com/Alims-Repo/SecureVault-KMP")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("alim")
                name.set("Alim Sourav")
                email.set("sourav.0.alim@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/Alims-Repo/SecureVault-KMP")
            connection.set("scm:git:git://github.com/Alims-Repo/SecureVault-KMP.git")
            developerConnection.set("scm:git:ssh://github.com/Alims-Repo/SecureVault-KMP.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
}

// Read signing credentials from ~/.gradle/gradle.properties
// Keys: signingInMemoryKeyId, signingInMemoryKey, signingInMemoryKeyPassword
val signingKeyId = findProperty("signingInMemoryKeyId") as String?
val signingKey = findProperty("signingInMemoryKey") as String?
val signingPassword = findProperty("signingInMemoryKeyPassword") as String? ?: ""

signing {
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        logger.warn("⚠️  signingInMemoryKey not set — publications will NOT be signed.")
    }
}