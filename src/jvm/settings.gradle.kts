plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "strictland"
include("compat-kotlin", "compat-scala")

dependencyResolutionManagement {
    repositories { mavenCentral() }
}
