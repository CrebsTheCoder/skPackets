plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

group = "org.crebs"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://maven.pvphub.me/tofaa")
    maven("https://repo.skriptlang.org/releases")
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.ShaneBeee")
        }
        metadataSources {
            mavenPom()
            artifact()
            ignoreGradleMetadataRedirection()
        }
    }
}

dependencies {
    implementation("io.github.tofaa2:spigot:4.0.0-alpha.4") {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    compileOnly("com.github.ShaneBeee:SkBee:3.23.0") {
        isTransitive = false
    }
    implementation("com.github.ShaneBeee:SkriptRegistration:1.5.0")
    compileOnly("io.papermc.paper:paper-api:26.3.build.195-stable")
    compileOnly("com.github.SkriptLang:Skript:2.17.1") {
        isTransitive = false
    }
    compileOnly("com.github.retrooper:packetevents-spigot:2.14.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("me.tofaa.entitylib", "org.crebs.skpackets.libs.entitylib")
    relocate("com.github.shanebeee.skr", "org.crebs.skpackets.libs.skr")
    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.test {
    enabled = false
}
