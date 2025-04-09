plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.ranull"
version = "4.9.8.1"
description = "Full featured lightweight death chest plugin."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
    maven {
        url = uri("https://repo.jeff-media.com/public")
    }
    maven {
        url = uri("https://repo.ranull.com/maven/ranull")
    }
    maven {
        url = uri("https://jitpack.io/")
    }
    maven {
        url = uri("https://maven.playpro.com/")
    }
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/groups/public/")
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://libraries.minecraft.net/")
    }
    maven {
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
    maven {
        url = uri("https://repo.oraxen.com/releases")
    }
    maven {
        url = uri("https://maven.citizensnpcs.co/repo")
    }
    maven {
        url = uri("https://repo.clojars.org/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        url = uri("https://repo.glaremasters.me/repository/bloodshot/")
    }
    maven {
        url = uri("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
    }
    maven {
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }
    maven {
        url = uri("https://repo.skriptlang.org/releases")
    }
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        url = uri("https://repo.minebench.de/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://repo.fancyplugins.de/releases")
    }
    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }
    mavenLocal()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.coreprotect:coreprotect:22.4")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    compileOnly("de.oliver:FancyNpcs:2.4.2")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    compileOnly("com.zaxxer:HikariCP:6.0.0")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    compileOnly("org.postgresql:postgresql:42.7.4")
    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("com.github.puregero:multilib:1.2.4")
    compileOnly("com.mojang:authlib:6.0.55-graves")
    compileOnly("com.ranull:skulltextureapi:1.0")
    compileOnly("dev.sergiferry:PlayerNPC:2023.6")
    compileOnly("com.mira:furnitureengine:3.3")
    compileOnly("com.github.MilkBowl:Vault:1.7.3")
    compileOnly("me.lokka30:treasury-api:2.0.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.2.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.6-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12")
    compileOnly("com.palmergames.bukkit.towny:towny:0.100.4.12")
    compileOnly("de.jeff_media:ChestSortAPI:13.0.0-SNAPSHOT")
    compileOnly("com.github.placeholderapi:placeholderapi:master-87ea46a4dd-1")
    compileOnly("com.griefdefender:api:2.0.0-20210822.184639-9")
    compileOnly("com.github.SkriptLang:Skript:2.11.0-pre1")
    compileOnly("com.github.Ste3et:FurnitureLib:3.2.6")
    compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.3-beta-14")
    compileOnly("com.github.jojodmo:ItemBridge:b0054538c1")
    compileOnly("net.kyori:adventure-text-minimessage:4.20.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.20.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.4")
    implementation("com.github.imDaniX:MiniTranslator:v2.1")
    compileOnly("commons-io:commons-io:2.17.0")
    compileOnly("de.themoep:minedown-adventure:1.7.3-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("org.apache.commons:commons-lang3:3.17.0")
    compileOnly("org.json:json:20240303")
    compileOnly("net.luckperms:api:5.4")
    implementation("com.tchristofferson:ConfigUpdater:2.1-SNAPSHOT")
    compileOnly("org.apache.maven:maven-resolver-provider:3.9.9")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:2.0.2")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.22")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.14.0")
    implementation("com.alessiodp:libby:2.0.1-graves")
    compileOnly("com.github.oshi:oshi-core:6.8.0")
    compileOnly("org.apache.commons:commons-text:1.12.0")
    compileOnly("com.microsoft.sqlserver:mssql-jdbc:12.10.0.jre8")
    compileOnly("com.github.koca2000:NoteBlockAPI:1.6.2")
    implementation("com.github.Anon8281:UniversalScheduler:0.1.6")
}

tasks.shadowJar {
    archiveClassifier.set("") // Makes shaded JAR the default output
    relocate("org.bstats", "${project.group}.${project.name}.libraries.bstats")
    relocate("com.github.puregero.multilib", "${project.group}.${project.name}.libraries.multilib")
    relocate("com.zaxxer.hikari", "${project.group}.${project.name}.libraries.hikari")
    relocate("com.tchristofferson.configupdater", "${project.group}.${project.name}.libraries.configupdater")
    relocate("de.themoep.minedown", "${project.group}.${project.name}.libraries.minedown")
    relocate("net.kyori", "${project.group}.${project.name}.libraries.kyori")
    relocate("me.imdanix.text", "${project.group}.${project.name}.libraries.imdanix.text")
    relocate("com.alessiodp.libby", "${project.group}.${project.name}.libraries.libby")
    relocate("org.h2", "${project.group}.${project.name}.libraries.h2")
    relocate("com.mysql", "${project.group}.${project.name}.libraries.mysql")
    relocate("org.postgresql", "${project.group}.${project.name}.libraries.postgresql")
    relocate("com.mariadb", "${project.group}.${project.name}.libraries.mariadb")
    relocate("com.microsoft", "${project.group}.${project.name}.libraries.microsoft")
    relocate("com.github.Anon8281.universalScheduler", "${project.group}.${project.name}.libraries.universalScheduler")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}