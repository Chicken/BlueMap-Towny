plugins {
    java
    id ("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "codes.antti"
version = "2.3.2"

repositories {
    mavenCentral()
    maven {
        setUrl ("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        setUrl ("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        setUrl ("https://repo.glaremasters.me/repository/towny/")
    }
    maven {
        setUrl ("https://jitpack.io/")
    }
    maven {
        setUrl ("https://repo.mikeprimm.com/")
    }
}

dependencies {
    compileOnly ("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly ("com.palmergames.bukkit.towny:towny:0.100.0.0")
    compileOnly ("com.github.BlueMap-Minecraft:BlueMapAPI:v2.4.0")
    compileOnly ("com.github.TownyAdvanced:SiegeWar:2.4.0")
    compileOnly ("us.dynmap:DynmapCoreAPI:3.4")
    implementation ("com.github.TechnicJelle:BMUtils:v4.2") {
        exclude ( group = "org.jetbrains", module = "annotations" )
        exclude ( group = "org.intellij.lang", module = "annotations" )
    }
}

val javaTarget = 17
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

tasks.processResources {
    from("src/main/resources") {
        include("plugin.yml")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
                "version" to project.version
        )
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8"
    }
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("nonshadow")
}

tasks.shadowJar {
    archiveClassifier.set("")

    relocate ("com.technicjelle", "codes.antti.bluemaptowny.shadow.jelle")
}
