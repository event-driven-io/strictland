plugins {
    scala
    alias(libs.plugins.spotless)
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf("-Yexplicit-nulls")
}

dependencies {
    implementation(libs.scala.library)
    implementation(project(":"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.scalatest)
    testRuntimeOnly("org.scalatestplus:junit-5-12_3:3.2.19.0")
    testRuntimeOnly("org.junit.platform:junit-platform-engine")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

spotless {
    scala {
        scalafmt(libs.versions.scalafmt.get()).configFile(rootProject.file(".scalafmt.conf"))
    }
    kotlinGradle {
        ktlint()
    }
}
