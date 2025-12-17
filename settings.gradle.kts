rootProject.name = "kmp-local-llm"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":core_llm")
include(":sampleShared")
include(":androidApp")
include(":desktopApp")
