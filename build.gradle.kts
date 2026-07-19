import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    base
    id("architectury-plugin") version "3.5.169" apply false
    id("dev.architectury.loom-no-remap") version "1.17.491" apply false
    kotlin("jvm") version "2.4.0" apply false
}

val modVersion: String by project
val mavenGroup: String by project

allprojects {
    group = mavenGroup
    version = modVersion
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://thedarkcolour.github.io/KotlinForForge/")
        maven("https://maven.isxander.dev/releases")
        maven("https://dl.cloudsmith.io/public/tslat/sbl/maven/")
        maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
            withSourcesJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(25)
            options.encoding = "UTF-8"
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_25)
                languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
                apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
                javaParameters.set(true)
                progressiveMode.set(true)
                jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }
    }
}

tasks.register("buildAll") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Builds both distributable loader artifacts."
    dependsOn(":fabric:build", ":neoforge:build")
}

gradle.projectsEvaluated {
    val neoForgeTasks = project(":neoforge").tasks

    tasks.register("runHarnesses") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs the complete executable and JUnit verification suite on NeoForge."
        dependsOn(neoForgeTasks.named("runHarnesses"))
    }

    listOf("runClient", "runGameTestServer").forEach { taskName ->
        neoForgeTasks.findByName(taskName)?.let { platformTask ->
            tasks.register(taskName) {
                group = "galactic wars"
                description = "Delegates to :neoforge:$taskName."
                dependsOn(platformTask)
            }
        }
    }
}
