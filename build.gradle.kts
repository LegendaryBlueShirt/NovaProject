plugins {
    kotlin("multiplatform") version "1.8.20"
    kotlin("plugin.serialization") version "1.8.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val nativelibs = ""
val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}\\.konan"
val resFile = file("$buildDir/konan/res/Nova.res")

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    nativelibs = when {
//        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> "/usr"
        isMingwX64 -> "C:\\mingw64"
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    linuxX64("linux").apply {
        binaries {
            executable("nova") {
                entryPoint = "com.justnopoint.nova.main"
                linkerOpts = mutableListOf(
                    "-L${nativelibs}/lib/x86_64-linux-gnu/cmake",
                    "-L${nativelibs}/lib/x86_64-linux-gnu", "-L${project.projectDir}/native/lib",
                    "-lXtst", "-lX11",
                    "-lSDL2")
            }
        }
        compilations.getByName("main").cinterops {
            val SDL by creating {
                includeDirs(
                    "${nativelibs}/include/x86_64-linux_gnu",
                    "${nativelibs}/include"
                )
                extraOpts = mutableListOf(
                    "-libraryPath", "${nativelibs}/lib/x86_64-linux-gnu/cmake",
                    "-libraryPath", "${nativelibs}/lib/x86_64-linux-gnu"
                )
            }
            val tinyfiledialogs by creating {
                includeDirs {
                    allHeaders("${nativelibs}/include", "${project.projectDir}/native/include")
                }
                extraOpts = mutableListOf(
                    "-libraryPath", "${nativelibs}/lib",
                    "-libraryPath", "${project.projectDir}/native/lib"
                )
            }
            val xtest by creating {
                includeDirs(
                    "${nativelibs}/include"
                )
                extraOpts = mutableListOf(
                    //"-libraryPath", "${nativelibs}/lib/x86_64-linux-gnu/cmake",
                    //"-libraryPath", "${nativelibs}/lib/x86_64-linux-gnu"
                )
            }
        }
    }

    mingwX64("native").apply {
        binaries {
            executable("nova") {
                entryPoint = "com.justnopoint.nova.main"
                linkerOpts = mutableListOf(
                    "$resFile",
                    "-L${nativelibs}\\lib",
                    "-L${nativelibs}\\bin", "-L${project.projectDir}\\native\\lib",
                    "-lmingw32", "-lSDL2main", "-lSDL2", "-lSDL2_image",
                    "-mwindows"
                )
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
            val tinyfiledialogs by creating {
                includeDirs {
                    allHeaders("${nativelibs}\\include", "${project.projectDir}\\native\\include")
                }
                extraOpts = mutableListOf(
                    "-libraryPath", "${nativelibs}\\lib",
                    "-libraryPath", "${project.projectDir}\\native\\lib"
                )
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
    register("runWinWithRes", Exec::class) {
        dependsOn("copyWinRes")

        workingDir("$buildDir/nova")
        commandLine("cmd", "/c", "nova.exe")
    }

    register("runLinWithRes", Exec::class) {
        dependsOn("copyLinRes")

        workingDir("$buildDir/nova")
        commandLine("./nova.kexe")
    }

    register("copyWinRes", Copy::class) {
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

    register("copyLinRes", Copy::class) {
        group = "other"
        description = "Copies the release exe and resources into one directory"

        from("$buildDir/processedResources/linux/main") {
            include("**/*")
        }

        from("$buildDir/bin/linux/novaDebugExecutable") {
            include("**/*")
        }

        into("$buildDir/nova")
        includeEmptyDirs = false
        dependsOn("linuxProcessResources")
        dependsOn("linkNovaDebugExecutableLinux")
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