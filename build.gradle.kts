plugins {
    kotlin("multiplatform") version "1.9.21" apply false
    kotlin("android") version "1.9.21" apply false
    id("com.android.library") version "8.2.0" apply false
}

allprojects {
    group = "com.thinq.kmp.llm"
    version = "0.1.0"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
