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
