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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")

        flatDir {
            dirs("${rootProject.projectDir}/unityLibrary/libs")
        }
    }
}

rootProject.name = "FoodAR"
include(":app", ":unityLibrary", ":unityLibrary:xrmanifest.androidlib", ":unityLibrary:FirebaseApp.androidlib")
 