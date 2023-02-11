plugins {
    kotlin("multiplatform") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val nativelibs = "C:\\mingw64"
val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}\\.konan"
val resFile = file("$buildDir/konan/res/Nova.res")

kotlin {
//    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")
//    val nativeTarget = when {
//        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        isMingwX64 -> mingwX64("native")
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
//    }

    linuxX64("linux").apply {
        binaries {
            executable("nova") {
                entryPoint = "com.justnopoint.nova.main"
                linkerOpts = mutableListOf("$resFile",
                    "-L${nativelibs}\\lib",
                    "-L${nativelibs}\\bin", "-L${project.projectDir}\\native\\lib",
                    "-lX11",
                    "-lmingw32", "-lSDL2main", "-lSDL2", "-lSDL2_image")
            }
        }
        compilations.getByName("main").cinterops {
            val SDL by creating {
                includeDirs {
                    allHeaders("${nativelibs}\\include")
                }
                extraOpts = mutableListOf(
                    "-libraryPath", "${nativelibs}\\lib",
                    "-libraryPath", "${nativelibs}\\bin"
                )
            }
        }
    }

    mingwX64("native").apply {
        binaries {
            executable("nova") {
                entryPoint = "com.justnopoint.nova.main"
                linkerOpts = mutableListOf("$resFile",
                    "-L${nativelibs}\\lib",
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
        val sdlMain by creating {
            dependsOn(commonMain.get())
        }
        val nativeMain by getting {
            dependsOn(sdlMain)
        }
        val linuxMain by getting {
            dependsOn(sdlMain)
        }
    }

    dependencies {
        commonMainImplementation(platform("com.squareup.okio:okio-bom:3.3.0"))
        commonMainImplementation("com.squareup.okio:okio")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.5.0-RC")
    }
}

tasks {
    register("runWithRes", Exec::class) {
        dependsOn("copyRes")

        workingDir("$buildDir/nova")
        commandLine("cmd", "/c", "nova.exe")
    }

    register("copyRes", Copy::class) {
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

    register("winRes", Exec::class) {
        val rcFile = file("Nova.rc")
        val path = System.getenv("PATH")
        val windresDir = "$konanUserDir\\dependencies\\msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1\\bin"
        executable("windres")
        args("-O", "coff", "-o", resFile, rcFile)
        environment("PATH", "$windresDir;$path")

        inputs.file(rcFile)
        outputs.file(resFile)
    }

    matching{ it.name == "linkNovaDebugExecutableNative" }.all{
        dependsOn("winRes")
        inputs.file(resFile)
    }
}