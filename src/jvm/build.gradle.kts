import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import me.champeau.gradle.japicmp.JapicmpTask

plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.japicmp)
}

group = "io.event-driven"
version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")

java {
    toolchain { languageVersion = JavaLanguageVersion.of(26) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

val testJdk = providers.gradleProperty("testJdk").map(String::toInt).getOrElse(26)
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(testJdk) })
}

dependencies {
    api(libs.jspecify)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    val baselineConfig = configurations.detachedConfiguration(
        dependencies.create("io.event-driven:strictland:$baseline")
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
