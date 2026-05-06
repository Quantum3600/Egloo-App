import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.compose.window.styler)
    implementation(libs.koin.compose)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.ktor.client.core)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.koin.core)
    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.trishit.egloo.MainKt"
        jvmArgs += listOf(
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Egloo"
            packageVersion = "1.0.0"
            description = "Your knowledge, safely stored"
            copyright = "© 2025 Egloo"
            macOS {
                bundleID = "com.egloo.desktop"
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "Egloo"
                upgradeUuid = "e4f3c2b1-a0d9-4e8f-b7c6-d5e4f3c2b1a0"
            }

        }
    }
}