plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjspecify-annotations=strict",
        )
    }
}

dependencies {
    implementation(project(":"))
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

spotless {
    kotlin {
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        ktlint()
    }
}
