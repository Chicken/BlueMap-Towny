plugins {
    java
    id ("com.github.johnrengelman.shadow") version "8.1.1"
    id ("com.modrinth.minotaur") version "2.+"
    id ("io.papermc.hangar-publish-plugin") version "0.0.5"
}

group = "codes.antti"
version = "2.3.0"

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
    implementation ("com.github.TechnicJelle:UpdateCheckerJava:v2.1") {
        exclude ( group = "org.jetbrains", module = "annotations" )
        exclude ( group = "org.intellij.lang", module = "annotations" )
    }
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

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("Y6O9cRjl")
    versionNumber.set(project.version as String)
    changelog.set("View the changelog at [GitHub releases](https://github.com/Chicken/BlueMap-Towny/releases/tag/${project.version})")
    uploadFile.set(tasks.findByName("shadowJar"))
    loaders.addAll("spigot", "paper", "folia")
    gameVersions.addAll(
            "1.16.5",
            "1.17", "1.17.1",
            "1.18", "1.18.1", "1.18.2",
            "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
            "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"
    )
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        namespace("Chicken", "BlueMap-Towny")
        channel.set("Release")
        changelog.set("View the changelog at [GitHub releases](https://github.com/Chicken/BlueMap-Towny/releases/tag/${project.version})")
        apiKey.set(System.getenv("HANGAR_TOKEN"))
        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                url.set("https://github.com/Chicken/BlueMap-Towny/releases/download/${project.version}/BlueMap-Towny-${project.version}.jar")
                dependencies.hangar("Blue", "BlueMap")
                dependencies.hangar("TownyAdvanced", "Towny")
                platformVersions.set(listOf(
                        "1.16.5",
                        "1.17", "1.17.1",
                        "1.18", "1.18.1", "1.18.2",
                        "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
                        "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"
                ))
            }
        }
    }
}

tasks.register("publish") {
    dependsOn("modrinth")
    dependsOn("publishPluginPublicationToHangar")
}
