plugins {
    kotlin("jvm") version "2.4.21"
}

group = "booker"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.rest-assured:rest-assured:5.5.7")
    testImplementation("io.rest-assured:kotlin-extensions:5.5.7")
    testImplementation("io.rest-assured:json-schema-validator:5.5.7")

    testImplementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("net.datafaker:datafaker:2.7.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.withType<Test>().configureEach {
    // Тесты ходят во внешний API: результат не определяется входами Gradle, кэшировать нечего
    outputs.upToDateWhen { false }
    // Пробрасываем -Dbooker.* из командной строки Gradle в JVM тестов
    System.getProperties()
        .filterKeys { it.toString().startsWith("booker.") }
        .forEach { (k, v) -> systemProperty(k.toString(), v) }
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        // В stdout пишется только HTTP-лог упавших тестов и seed — показываем его в консоли
        showStandardStreams = true
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("known-bug")
    }
}

// Тесты известных дефектов проверяют правильное поведение и сейчас падают.
// PASSED здесь означает, что дефект исправлен и ожидания в основном наборе пора обновить.
val knownBugs by tasks.registering(Test::class) {
    description = "Runs tests of known API defects (expected to fail until the defects are fixed)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("known-bug")
    }
    ignoreFailures = true
}
