import kotlin.io.path.Path
import kotlin.io.path.exists

plugins {
    id("application")
    id("net.neoforged.moddev") version "2.0.148" // https://plugins.gradle.org/plugin/net.neoforged.moddev
    id("idea")
    id("com.dorongold.task-tree") version "4.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    jarJar(implementation("com.fasterxml.jackson.core:jackson-databind:${property("jackson_databind_version")!!}")!!)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(property("target_java_version")!! as String))

neoForge {
    version = property("neoforge_version")!! as String

    parchment {
        mappingsVersion = property("parchment_version")!! as String
        minecraftVersion = property("minecraft_version")!! as String
    }

    runs {
        create("client") {
            client()
            gameDirectory = rootProject.file("runs/client")
        }
        create("clientAuth") {
            client()
            gameDirectory = rootProject.file("runs/client")
            devLogin = true
        }
        create("server") {
            server()
            gameDirectory = rootProject.file("runs/server")
        }

        configureEach {
            // Recommended logging level for the console
            // You can set various levels here.
            // Please read: https://stackoverflow.com/questions/2031163/when-to-use-the-different-log-levels
            logLevel = org.slf4j.event.Level.DEBUG

            jvmArguments.addAll(
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowEnhancedClassRedefinition",
                "-XX:+EnableDynamicAgentLoading",
                "-XX:HotswapAgent=fatjar",
                //jvmArgument("-XX:-OmitStackTraceInFastThrow", // uncomment when you get exceptions with null messages etc
                // "-XX:+UnlockCommercialFeatures", // uncomment for profiling
            )

            systemProperties.putAll(mapOf(
                "mixin.debug.export" to "true",
                "mixin.debug.verbose" to "true",
                /**
                 * Recommended logging data for a userdev environment
                 * The markers can be added/remove as needed separated by commas.
                 * "SCAN": For mods scan.
                 * "REGISTRIES": For firing of registry events.
                 * "REGISTRYDUMP": For getting the contents of all registries.
                 */
                "forge.logging.markers" to "REGISTRIES"
            ))

            Path(System.getProperty("java.home"), "lib/hotswap/hotswap-agent.jar")
                .takeIf { it.exists() }
                .let {
                    logger.lifecycle("Found Hotswap agent in Java home. Adding to JVM arguments...: $it")
                    jvmArgument("-javaagent:$it")
                }
        }
    }
}

application {
    mainClass.set(property("main_class")!! as String)
}

idea.module {
    isDownloadSources = true
//    isDownloadJavadoc = true
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
    }

    processResources {
        val replaceProperties = mapOf(
            "mod_id" to project.property("mod_id")!!,
            "mod_name" to project.property("mod_name")!!,
            "mod_version" to version,
            "mod_license" to project.property("mod_license")!!,
            "minecraft_version" to project.property("minecraft_version")!!,
            "minecraft_version_range" to project.property("minecraft_version_range")!!,
            "loader_version" to project.property("neoforge_version")!!,
            "loader_version_range" to project.property("neoforge_version_range")!!,
            "java_version" to project.property("target_java_version")!!,
            "description" to project.description,
        )
        inputs.properties(replaceProperties)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(replaceProperties)
        }
    }

    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveBaseName.set(project.property("archive_base_name") as String)
        archiveVersion.set("$version")
    }

    jar {
        manifest {
            attributes["Main-Class"] = project.property("main_class") as String
        }
    }
}
