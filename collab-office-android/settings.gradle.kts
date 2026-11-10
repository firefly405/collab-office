pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "collab-office-android"

include(":app")

// Core modules
include(":core:network")
include(":core:database")
include(":core:model")
include(":core:common")

// Feature modules
include(":feature:auth")
include(":feature:document")
include(":feature:chat")
include(":feature:meeting")
include(":feature:task")
include(":feature:file")
include(":feature:notification")
include(":feature:search")
