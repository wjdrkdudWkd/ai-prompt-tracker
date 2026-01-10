plugins {
    `java-library`
    `maven-publish`
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

// Group inherited from root project
// Version inherited from root project (see build.gradle.kts in root)

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
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
}

tasks.test {
    useJUnitPlatform()
}

// Disable Spring Boot's bootJar task (this is a library, not an app)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// Enable standard jar task
tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}

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

            pom {
                name.set("AI Prompt Tracker Spring Boot Starter")
                description.set("Spring Boot starter for tracking AI API calls with embedded dashboard")
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
