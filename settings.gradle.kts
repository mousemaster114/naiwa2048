pluginManagement {
    resolutionStrategy { eachPlugin {
        if (requested.id.id == "com.android.application") useModule("com.android.tools.build:gradle:${requested.version}")
        if (requested.id.id == "org.jetbrains.kotlin.android") useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
    } }
    repositories {
        System.getenv("NAIWA_MAVEN_REPO")?.let { maven { url = uri(it) } }
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        System.getenv("NAIWA_MAVEN_REPO")?.let { maven { url = uri(it) } }
        google(); mavenCentral()
    }
}
rootProject.name = "Naiwa2048"
include(":app")
