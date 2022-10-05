plugins {
    kotlin("multiplatform") version "1.7.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val nativelibs = "C:\\mingw64"

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable("nova") {
                entryPoint = "com.justnopoint.nova.main"
                linkerOpts = mutableListOf("-L${nativelibs}\\lib",
                    "-L${nativelibs}\\bin", "-L${project.projectDir}\\native\\lib",
                    "-lmingw32", "-lSDL2main", "-lSDL2", "-lSDL2_image",
                    "-mwindows")
            }
        }
        compilations.getByName("main").cinterops {
            val SDL by creating {
                includeDirs {
                    allHeaders("${nativelibs}\\include")
                }
                extraOpts = mutableListOf("-libraryPath", "${nativelibs}\\lib",
                    "-libraryPath", "${nativelibs}\\bin")
            }
            val tinyfiledialogs by creating {
                includeDirs {
                    allHeaders("${nativelibs}\\include", "${project.projectDir}\\native\\include")
                }
                extraOpts = mutableListOf("-libraryPath", "${nativelibs}\\lib",
                    "-libraryPath", "${project.projectDir}\\native\\lib")
            }
        }
    }
    sourceSets {
        val nativeMain by getting
        val nativeTest by getting
    }

    dependencies {
        commonMainImplementation(platform("com.squareup.okio:okio-bom:3.2.0"))
        commonMainImplementation("com.squareup.okio:okio")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    }
}

tasks {
    val runWithRes = register("runWithRes", Exec::class) {
        dependsOn("copyRes")

        workingDir("$buildDir/nova")
        commandLine("cmd", "/c", "nova.exe")
    }

    val copyRes = register("copyRes", Copy::class) {
        group = "other"
        description = "Copies the release exe and resources into one directory"

        from("$buildDir/processedResources/native/main") {
            include("**/*")
        }

        from("$buildDir/bin/native/novaDebugExecutable") {
            include("**/*")
        }

        from("${project.projectDir}\\tinyfd") {
            include("tinyfiledialogs64.dll")
        }

        into("$buildDir/nova")
        includeEmptyDirs = false
        dependsOn("nativeProcessResources")
        dependsOn("linkNovaDebugExecutableNative")
    }
}