dependencies {
    // AI Prompt Tracker Starter (provides all tracking functionality)
    implementation(project(":tracker-starter"))

    // WebFlux for WebClient (needed by demo controller)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Database for demo
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Monitoring (optional)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
    enabled = true
    archiveBaseName.set("ai-prompt-tracker-demo")
    archiveVersion.set(project.version.toString())
}
