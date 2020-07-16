plugins {
    java
    application
    checkstyle
    jacoco
}

group = "org.mitre"
version = "0.0.1"

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
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.validation", "4.3.1-SNAPSHOT")

    // validator dependencies (should be able to get these automatically?)
    implementation("org.apache.commons","commons-compress", "1.19")
    implementation("org.apache.httpcomponents", "httpclient", "4.5.10")
    implementation("org.fhir", "ucum", "1.0.2")

    // GSON for our JSON needs
    implementation("com.google.code.gson", "gson", "2.8.6")

    implementation("org.slf4j", "slf4j-log4j12", "1.7.30")

    // Web Server
    implementation("com.sparkjava", "spark-core", "2.9.1")

    // Testing stuff
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.5.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "org.mitre.inferno.App"
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

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "JNferno"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "org.mitre.inferno.rest.Validate"
    }
    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
