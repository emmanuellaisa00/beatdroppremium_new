pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor is distributed via JitPack only — no Maven Central release.
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "BeatDrop"
include(":app")
