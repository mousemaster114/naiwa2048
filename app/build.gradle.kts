plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.naiwa.game"
    compileSdk = 34
    buildToolsVersion = "36.1.0"
    defaultConfig { applicationId = "com.naiwa.game"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
}

// Allows direct JUnit execution on Windows hosts whose Java argfile encoding
// cannot represent the Gradle worker path. Normal builds use testDebugUnitTest.
tasks.register("exportTestClasspath") {
    dependsOn("compileDebugUnitTestKotlin", "processDebugUnitTestJavaRes", "generateDebugUnitTestConfig", "bundleDebugClassesToRuntimeJar")
    doLast {
        val testTask = tasks.named<Test>("testDebugUnitTest").get()
        layout.buildDirectory.file("test-classpath.txt").get().asFile.writeText(testTask.classpath.asPath)
    }
}
