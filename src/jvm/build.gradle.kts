import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import me.champeau.gradle.japicmp.JapicmpTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-library`
    jacoco
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.japicmp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.palantir.java.format.idea)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
}

group = "io.event-driven"
version = providers.gradleProperty("version").getOrElse("0.2.0-SNAPSHOT")

java {
    toolchain { languageVersion = JavaLanguageVersion.of(26) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        excludedPaths = ".*/build/generated/.*"
        error("NullAway")
    }
}

nullaway {
    onlyNullMarked = true
}

val testJdk = providers.gradleProperty("testJdk").map(String::toInt).getOrElse(26)
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(testJdk) })
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).apply {
        // ApprovalTests ships its .java sources inside its jars. With those on the
        // compile classpath, javadoc would implicitly compile them while resolving
        // our own API, failing because they import Gson, which we don't depend on.
        // An empty sourcepath stops javadoc from reading those bundled sources; our
        // own sources are still documented as they're passed as explicit file args.
        addStringOption("sourcepath", "")
        // Treat javadoc warnings (e.g. missing comments on public API) as errors.
        addBooleanOption("Xwerror", true)
    }
}

tasks.named("check") { dependsOn("jacocoTestReport", "jacocoTestCoverageVerification", "javadoc") }

dependencies {
    api(libs.jspecify)
    implementation(libs.classgraph)
    api(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.approvaltests)
    implementation(platform(libs.junit.bom))
    implementation(libs.junit.jupiter)
    errorprone(libs.errorprone.core)
    errorprone(libs.errorprone.contrib)
    errorprone(libs.errorprone.refaster)
    errorprone(libs.nullaway)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        palantirJavaFormat(
            libs.versions.palantir.java.format
                .get(),
        )
        removeUnusedImports()
        formatAnnotations()
    }
    kotlinGradle {
        ktlint()
    }
}

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral(true)
    signAllPublications()
    coordinates("io.event-driven", "strictland", version.toString())
    pom {
        name = "Strictland"
        description = "Fluent DSL for testing event schema versioning and serialization compatibility"
        inceptionYear = "2026"
        url = "https://github.com/event-driven-io/strictland"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "oskardudycz"
                name = "Oskar Dudycz"
                email = "oskar.dudycz@gmail.com"
            }
        }
        scm {
            url = "https://github.com/event-driven-io/strictland"
            connection = "scm:git:git://github.com/event-driven-io/strictland.git"
            developerConnection = "scm:git:ssh://git@github.com/event-driven-io/strictland.git"
        }
    }
}

val baseline = providers.gradleProperty("baselineVersion").orNull?.takeIf { it.isNotBlank() }
if (baseline != null) {
    val baselineConfig =
        configurations.detachedConfiguration(
            dependencies.create("io.event-driven:strictland:$baseline"),
        )
    tasks.register<JapicmpTask>("japicmp") {
        oldClasspath.from(baselineConfig)
        newClasspath.from(tasks.named<Jar>("jar").map { it.archiveFile })
        failOnModification = true
        failOnSourceIncompatibility = true
        ignoreMissingClasses = false
        txtOutputFile = layout.buildDirectory.file("reports/japicmp/compatibility.txt")
    }
    tasks.named("check") { dependsOn("japicmp") }
}
