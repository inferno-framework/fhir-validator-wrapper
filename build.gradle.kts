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
    // https://chat.fhir.org/#narrow/stream/179166-implementers/topic/New.20validator.20JAR.20location
    // the ig-publisher uses this one too
    // https://github.com/HL7/fhir-ig-publisher/blob/master/pom.xml#L68
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.validation", "5.5.10")

    // validator dependencies (should be able to get these automatically?)
    implementation("org.apache.commons","commons-compress", "1.19")
    implementation("org.apache.httpcomponents", "httpclient", "4.5.10")
    implementation("org.fhir", "ucum", "1.0.2")
    implementation("com.squareup.okhttp3", "okhttp", "4.9.0")

    // GSON for our JSON needs
    implementation("com.google.code.gson", "gson", "2.8.6")

    implementation("org.slf4j", "slf4j-log4j12", "1.7.30")

    // Web Server
    implementation("com.sparkjava", "spark-core", "2.9.1")

    // Testing stuff
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.5.2")
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
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
