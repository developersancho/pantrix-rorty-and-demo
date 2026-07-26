pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Pantrix SDK + its Gradle plugin. A raw-content Maven repo on the distribution repo's
        // `maven-repo` branch — the group filter keeps every other dependency off it.
        maven {
            url = uri("https://raw.githubusercontent.com/developersancho/pantrix-sdk-android-aar/maven-repo/")
            content { includeGroupByRegex("com\\.pantrix.*") }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pantrix SDK + its Gradle plugin. A raw-content Maven repo on the distribution repo's
        // `maven-repo` branch — the group filter keeps every other dependency off it.
        maven {
            url = uri("https://raw.githubusercontent.com/developersancho/pantrix-sdk-android-aar/maven-repo/")
            content { includeGroupByRegex("com\\.pantrix.*") }
        }
    }
}

rootProject.name = "pantrix-rorty-and-demo"
include(":app")
