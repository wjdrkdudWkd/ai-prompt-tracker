import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

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
 * Verify JAR contains React dashboard artifacts (lenient mode)
 *
 * This task verifies that the built JAR includes the embedded React dashboard.
 * Missing React files will produce warnings but won't fail the build.
 *
 * Usage: ./gradlew :tracker-starter:verifyJarContents
 */
val verifyJarContents by tasks.registering {
    group = "verification"
    description = "Verify JAR contains embedded React dashboard (warnings only)"

    dependsOn(tasks.jar)

    doLast {
        verifyJarContentsImpl(strict = false)
    }
}

/**
 * Verify JAR contains React dashboard artifacts (strict mode for CI)
 *
 * This task verifies that the built JAR includes the embedded React dashboard.
 * Missing React files will cause the build to FAIL (required for CI/release builds).
 *
 * Usage: ./gradlew :tracker-starter:verifyJarContentsStrict
 */
val verifyJarContentsStrict by tasks.registering {
    group = "verification"
    description = "Verify JAR contains embedded React dashboard (strict - fails on missing files)"

    dependsOn(tasks.jar)

    doLast {
        verifyJarContentsImpl(strict = true)
    }
}

/**
 * Common implementation for JAR verification
 *
 * @param strict If true, throws GradleException when React files are missing.
 *               If false, only logs warnings.
 */
fun verifyJarContentsImpl(strict: Boolean) {
    val jarFile = tasks.jar.get().archiveFile.get().asFile

    if (!jarFile.exists()) {
        throw GradleException("JAR file not found: ${jarFile.absolutePath}")
    }

    logger.lifecycle("")
    logger.lifecycle("════════════════════════════════════════════════════════════")
    logger.lifecycle("  Verifying JAR Contents ${if (strict) "(STRICT MODE)" else "(Lenient)"}")
    logger.lifecycle("════════════════════════════════════════════════════════════")
    logger.lifecycle("JAR: ${jarFile.name}")
    logger.lifecycle("")

    val jarEntries = mutableListOf<String>()
    ZipFile(jarFile).use { zipFile ->
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            jarEntries.add(entry.name)
        }
    }

    // Required files for React dashboard
    val requiredFiles = listOf(
        "META-INF/resources/aiprompt-tracker/index.html",
        "META-INF/resources/aiprompt-tracker/_next/"
    )

    // Optional file (vanilla fallback - always present)
    val fallbackFile = "META-INF/resources/aiprompt-tracker/dashboard-mvp.html"

    val missingFiles = mutableListOf<String>()
    val foundFiles = mutableListOf<String>()

    // Check React dashboard files
    for (required in requiredFiles) {
        val found = jarEntries.any { it.startsWith(required) }
        if (found) {
            foundFiles.add(required)
        } else {
            missingFiles.add(required)
        }
    }

    // Check fallback file
    val hasFallback = jarEntries.contains(fallbackFile)
    if (hasFallback) {
        foundFiles.add(fallbackFile)
    }

    // Report findings
    if (foundFiles.isNotEmpty()) {
        logger.lifecycle("✅ Found embedded UI files:")
        foundFiles.forEach { logger.lifecycle("   - $it") }
    }

    if (missingFiles.isNotEmpty()) {
        val message = buildString {
            appendLine("")
            appendLine("❌ Missing React dashboard files:")
            missingFiles.forEach { appendLine("   - $it") }
            appendLine("")
            appendLine("   The JAR will fall back to vanilla dashboard (dashboard-mvp.html)")
            appendLine("   To include React dashboard:")
            appendLine("   1. cd frontend && npm ci && npm run build")
            appendLine("   2. ./gradlew :tracker-starter:copyFrontend")
            appendLine("   3. ./gradlew :tracker-starter:build")
            appendLine("")
        }

        if (strict) {
            logger.error(message)
            logger.lifecycle("════════════════════════════════════════════════════════════")
            logger.lifecycle("")
            throw GradleException(
                "JAR verification FAILED: React dashboard is missing from JAR. " +
                "This is required for CI/release builds. See error details above."
            )
        } else {
            logger.warn(message)
        }
    } else {
        logger.lifecycle("")
        logger.lifecycle("✅ JAR verification passed: React dashboard is embedded")
    }

    logger.lifecycle("════════════════════════════════════════════════════════════")
    logger.lifecycle("")

    // Count files in each category
    val uiFiles = jarEntries.count { it.startsWith("META-INF/resources/aiprompt-tracker/") }
    logger.lifecycle("Total UI files in JAR: $uiFiles")
    logger.lifecycle("")
}

/**
 * CI Integration Point
 *
 * This configuration enables automatic frontend integration in CI environments
 * while keeping local builds fast and Node.js-optional.
 *
 * Strategy:
 * - Local: Frontend build is manual (./gradlew :tracker-starter:copyFrontend)
 * - CI: Explicitly runs copyFrontend before build (see CI workflow)
 *
 * The processResources dependency is intentionally NOT enabled by default to:
 * 1. Keep local builds fast (frontend build is slow)
 * 2. Allow consumers to build without Node.js
 * 3. Make CI builds explicit and traceable
 *
 * CI Workflow Example:
 *   - cd frontend && npm ci && npm run build
 *   - ./gradlew :tracker-starter:copyFrontend
 *   - ./gradlew :tracker-starter:build
 *   - ./gradlew :tracker-starter:verifyJarContents
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
