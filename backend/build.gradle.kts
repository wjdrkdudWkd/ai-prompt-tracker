dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Elasticsearch (선택적)
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // YAML 처리
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    // HTTP Client
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:elasticsearch")
}

tasks.bootJar {
    archiveBaseName.set("ai-prompt-tracker")
    archiveVersion.set(project.version.toString())
}
