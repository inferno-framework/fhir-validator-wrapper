plugins {
    java
    application
    checkstyle
    jacoco
}

group = "org.mitre"

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.validation", "6.5.17")

    // validator dependency needed for terminology (why can't it get this automatically?)
    implementation("com.squareup.okhttp3", "okhttp", "4.12.0")

    // validator dependency needed to prevent a ClassNotFoundException
    implementation("org.fhir:ucum:1.0.9")

    // GSON for our JSON needs
    implementation("com.google.code.gson", "gson", "2.11.0")

    // Basic logging. Reload4J is a security-focused fork of Log4J 1.x
    implementation("org.slf4j", "slf4j-reload4j", "2.0.16")

    // Web Server
    implementation("com.sparkjava", "spark-core", "2.9.4")

    // Testing stuff
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.9.3")
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("org.mitre.inferno.App")
}

checkstyle {
    toolVersion = "8.25"
}

jacoco {
    toolVersion = "0.8.8"
}

tasks {
  withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
  }
}

tasks.test {
    environment(mapOf("DISABLE_TX" to "true"))
    maxHeapSize = "6144m"
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    configure<JacocoTaskExtension> {
        excludes = mutableListOf("org/hl7/fhir/r4/formats/JsonParser",
                "org/hl7/fhir/r5/formats/JsonParser",
                "org/hl7/fhir/r4/formats/XmlParser",
                "org/hl7/fhir/r5/formats/XmlParser")
    }
}

val testCoverage by tasks.registering {
    group = "verification"
    description = "Runs the unit tests with coverage."

    dependsOn(":test", ":jacocoTestReport")
    val jacocoTestReport = tasks.findByName("jacocoTestReport")
    jacocoTestReport?.mustRunAfter(tasks.findByName("test"))
}


tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE

    manifest {
        attributes("Implementation-Title" to "FHIR Validator Wrapper",
            "Implementation-Version" to project.version,
            "Main-Class" to "org.mitre.inferno.App")
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

val setVersion = tasks.processResources {
    expand("version" to project.version)
    inputs.property("appVersion", project.version)
}
