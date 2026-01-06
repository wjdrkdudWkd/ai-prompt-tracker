dependencies {
    // AI Prompt Tracker Starter (provides all tracking functionality)
    implementation(project(":tracker-starter"))

    // Database for demo
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Monitoring (optional)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    enabled = true
    archiveBaseName.set("ai-prompt-tracker-demo")
    archiveVersion.set(project.version.toString())
}
