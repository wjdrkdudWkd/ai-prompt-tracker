import java.util.concurrent.TimeUnit

plugins {
    `java-library`
    `maven-publish`
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

// Group inherited from root project
// Version inherited from root project (see build.gradle.kts in root)

java {
    // Explicit Java 17 toolchain for consistent builds
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    // Target Java 17 bytecode for maximum compatibility
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Publish sources and javadoc jars (Maven Central and best practices)
    withSourcesJar()
    withJavadocJar()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // Spring Boot Auto-Configuration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")

    // Spring Boot Starters (required)
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-web")

    // WebFlux for WebClient support
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // JPA and Flyway (required for API - Pageable, etc.)
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.flywaydb:flyway-core")
    compileOnly("org.flywaydb:flyway-database-postgresql")
    compileOnly("com.h2database:h2")

    // OkHttp (optional)
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters"))

    // Use --release 17 flag for proper bytecode and API compatibility
    // This ensures the compiled bytecode works correctly on Java 17+ runtimes
    // and prevents use of newer Java APIs accidentally
    options.release.set(17)
}

tasks.test {
    useJUnitPlatform()
}

// Configure javadoc to be lenient (don't fail on warnings)
tasks.withType<Javadoc> {
    options {
        this as StandardJavadocDocletOptions
        addStringOption("Xdoclint:none", "-quiet")
    }
}

// Standard jar task is enabled by default for java-library plugin
// No need to configure bootJar since Spring Boot plugin is not applied to this module

// ════════════════════════════════════════════════════════════
// Frontend Build Integration
// ════════════════════════════════════════════════════════════

/**
 * Check if Node.js is available on the system
 *
 * This function safely checks for Node.js without causing Gradle configuration errors.
 * Returns true if `node --version` succeeds, false otherwise.
 */
fun isNodeAvailable(): Boolean {
    return try {
        val process = ProcessBuilder("node", "--version")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(5, TimeUnit.SECONDS)
        process.exitValue() == 0
    } catch (e: Exception) {
        false
    }
}

/**
 * Check if frontend build output exists
 */
fun hasFrontendBuildOutput(): Boolean {
    val outDir = file("../frontend/out")
    val indexHtml = file("../frontend/out/index.html")
    return outDir.exists() && indexHtml.exists()
}

/**
 * Build React dashboard (Next.js) if Node.js is available
 *
 * This task is safe to run even without Node.js installed.
 * It will skip gracefully with a warning message.
 */
val buildFrontend by tasks.registering(Exec::class) {
    group = "build"
    description = "Build Next.js dashboard for embedding in starter"

    workingDir = file("../frontend")

    val nodeAvailable = isNodeAvailable()

    if (nodeAvailable) {
        // Node.js is available - run the build
        commandLine("npm", "run", "build")

        doFirst {
            logger.lifecycle("")
            logger.lifecycle("════════════════════════════════════════════════════════════")
            logger.lifecycle("  Building React Dashboard (Next.js)")
            logger.lifecycle("════════════════════════════════════════════════════════════")
        }

        doLast {
            logger.lifecycle("")
            logger.lifecycle("✅ Frontend build complete")
            logger.lifecycle("")
        }
    } else {
        // Node.js not available - skip gracefully
        commandLine("echo", "Skipping frontend build - Node.js not available")

        doFirst {
            logger.warn("")
            logger.warn("⚠️  Node.js not found - skipping frontend build")
            logger.warn("   React dashboard will not be included in this build")
            logger.warn("")
            logger.warn("   To include the dashboard in your build:")
            logger.warn("   1. Install Node.js 18.18+ (https://nodejs.org/)")
            logger.warn("   2. cd frontend && npm install && npm run build")
            logger.warn("   3. Run: ./gradlew :tracker-starter:copyFrontend")
            logger.warn("")
        }
    }
}

/**
 * Copy React build output to starter resources
 *
 * This task copies the Next.js static export from frontend/out/
 * to the starter's resources directory where Spring Boot will serve it.
 *
 * Safe to run even if build output doesn't exist - will skip with warning.
 */
val copyFrontend by tasks.registering(Copy::class) {
    group = "build"
    description = "Copy Next.js build output to starter resources"

    dependsOn(buildFrontend)

    val outDir = file("../frontend/out")
    val targetDir = file("src/main/resources/META-INF/resources/aiprompt-tracker")

    // Only copy if build output exists
    onlyIf {
        val exists = hasFrontendBuildOutput()
        if (!exists) {
            logger.warn("")
            logger.warn("⚠️  Frontend build output not found at: ${outDir.absolutePath}")
            logger.warn("   Skipping copy. Run './gradlew :tracker-starter:buildFrontend' first")
            logger.warn("")
        }
        exists
    }

    from(outDir) {
        include("**/*")
        // Exclude dashboard-mvp.html if it exists in out/ (we preserve the one in resources)
        exclude("dashboard-mvp.html")
    }
    into(targetDir)

    // Preserve dashboard-mvp.html (vanilla fallback) if it exists
    doFirst {
        val mvpFile = file("${targetDir}/dashboard-mvp.html")
        if (mvpFile.exists()) {
            logger.lifecycle("Preserving dashboard-mvp.html (vanilla fallback)")
            copy {
                from(mvpFile)
                into(temporaryDir)
                rename { "dashboard-mvp.html.backup" }
            }
        }
    }

    doLast {
        // Restore preserved dashboard-mvp.html
        val mvpBackup = file("${temporaryDir}/dashboard-mvp.html.backup")
        if (mvpBackup.exists()) {
            copy {
                from(mvpBackup)
                into(targetDir)
                rename { "dashboard-mvp.html" }
            }
        }

        logger.lifecycle("")
        logger.lifecycle("✅ React dashboard copied to starter resources")
        logger.lifecycle("   Target: ${targetDir.absolutePath}")
        logger.lifecycle("")
    }
}

/**
 * CI Integration Point
 *
 * In CI environments, enable automatic frontend build by uncommenting below.
 * For local development, frontend build is optional (manual).
 *
 * Recommendation:
 * - Local: Manual build via `./gradlew :tracker-starter:copyFrontend`
 * - CI: Uncomment to include frontend in every build
 */
// tasks.named("processResources") {
//     dependsOn(copyFrontend)
// }

// Log version on build for visibility
tasks.register("printVersion") {
    doLast {
        println("📦 Building: ${project.group}:${project.name}:${project.version}")
    }
}

// Automatically print version before publishing
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn("printVersion")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            // Explicit artifact ID
            artifactId = "ai-prompt-tracker-starter"

            // Version is automatically inherited from project.version
            // Set via: ./gradlew publish -Pversion=1.2.3
            // or defined in gradle.properties

            // Publish resolved versions for dependencies
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            // Configure variant attributes for stable resolution across different Java versions
            // This ensures consumers using Java 21 toolchain can resolve this Java 17 library
            // without "selected by rule" warnings or variant mismatches
            suppressAllPomMetadataWarnings()

            pom {
                name.set("AI Prompt Tracker Spring Boot Starter")
                description.set("Spring Boot starter for tracking API calls with embedded dashboard")
                url.set("https://github.com/wjdrkdudWkd/ai-prompt-tracker")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("galoong")
                        name.set("Galoong")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/wjdrkdudWkd/ai-prompt-tracker.git")
                    developerConnection.set("scm:git:ssh://github.com:wjdrkdudWkd/ai-prompt-tracker.git")
                    url.set("https://github.com/wjdrkdudWkd/ai-prompt-tracker")
                }
            }
        }
    }

    // Optionally disable Gradle Module Metadata if it causes variant resolution issues
    // Uncomment the line below if consumers experience "selected by rule" or variant conflicts
    // This forces pure Maven POM resolution (more predictable for cross-version compatibility)
    // tasks.withType<GenerateModuleMetadata> {
    //     enabled = false
    // }

    repositories {
        maven {
            name = "Local"
            url = uri(layout.buildDirectory.dir("repo"))
        }

        // GitHub Packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/wjdrkdudWkd/ai-prompt-tracker")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
            }
        }
    }
}
