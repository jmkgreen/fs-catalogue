plugins {
    application
    checkstyle
    jacoco
}

group = "com.fscatalogue"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.fscatalogue.cli.CatalogApp")
    applicationName = "catalog"
}

dependencies {
    implementation("info.picocli:picocli:4.7.6")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("org.yaml:snakeyaml:2.2")


    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

checkstyle {
    toolVersion = "10.17.0"
    configDirectory.set(file("config/checkstyle"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}
