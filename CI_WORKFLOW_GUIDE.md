# CI Workflow Guide for AI Prompt Tracker

This guide provides example CI workflows for building and publishing the tracker-starter with embedded React dashboard.

## Overview

**Goal**: Release JAR always includes React dashboard (Option A)

**Strategy**:
- Frontend build happens in CI (not locally)
- Source code is versioned, build artifacts are not
- JAR is verified to contain React dashboard before publish

---

## GitHub Actions Workflow

### Complete Build & Publish Workflow

Create `.github/workflows/publish.yml`:

```yaml
name: Build and Publish

on:
  push:
    branches: [ main, develop ]
    tags:
      - 'v*'
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      # 1. Checkout code
      - name: Checkout code
        uses: actions/checkout@v4

      # 2. Setup Node.js for frontend build
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18.18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      # 3. Setup Java for backend build
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      # 4. Build React dashboard
      - name: Build Frontend (React Dashboard)
        run: |
          cd frontend
          npm ci
          npm run build
        env:
          NODE_ENV: production

      # 5. Copy frontend to starter resources
      - name: Copy Frontend to Starter
        run: |
          ./gradlew :tracker-starter:copyFrontend --no-daemon

      # 6. Build tracker-starter JAR
      - name: Build Starter JAR
        run: |
          ./gradlew :tracker-starter:build --no-daemon
        env:
          GRADLE_OPTS: "-Dorg.gradle.daemon=false"

      # 7. Verify JAR contains React dashboard
      - name: Verify JAR Contents
        run: |
          ./gradlew :tracker-starter:verifyJarContents --no-daemon

          # Alternative: Manual verification with jar command
          # JAR_FILE=$(find tracker-starter/build/libs -name "*.jar" | head -1)
          # echo "Checking JAR: $JAR_FILE"
          # jar tf "$JAR_FILE" | grep "META-INF/resources/aiprompt-tracker/index.html" || exit 1
          # jar tf "$JAR_FILE" | grep "META-INF/resources/aiprompt-tracker/_next" || exit 1

      # 8. Run tests
      - name: Run Tests
        run: |
          ./gradlew test --no-daemon

      # 9. Publish to GitHub Packages (on tag push)
      - name: Publish to GitHub Packages
        if: startsWith(github.ref, 'refs/tags/v')
        run: |
          ./gradlew :tracker-starter:publish -Pversion=${GITHUB_REF#refs/tags/v} --no-daemon
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITHUB_ACTOR: ${{ github.actor }}

      # 10. Upload JAR as artifact (for inspection)
      - name: Upload JAR Artifact
        uses: actions/upload-artifact@v4
        with:
          name: tracker-starter-jar
          path: tracker-starter/build/libs/*.jar
          retention-days: 7
```

---

## Verification Commands

### Local Verification (Before Pushing)

```bash
# 1. Clean build
./gradlew clean

# 2. Build frontend
cd frontend
npm ci
npm run build
cd ..

# 3. Copy to starter
./gradlew :tracker-starter:copyFrontend

# 4. Build JAR
./gradlew :tracker-starter:build

# 5. Verify JAR contents
./gradlew :tracker-starter:verifyJarContents

# 6. Manual JAR inspection (optional)
jar tf tracker-starter/build/libs/tracker-starter.jar | grep "aiprompt-tracker"
```

### Expected Output

```
✅ Found embedded UI files:
   - META-INF/resources/aiprompt-tracker/index.html
   - META-INF/resources/aiprompt-tracker/_next/
   - META-INF/resources/aiprompt-tracker/dashboard-mvp.html

✅ JAR verification passed: React dashboard is embedded

Total UI files in JAR: 72
```

---

## GitLab CI Example

### `.gitlab-ci.yml`

```yaml
stages:
  - build
  - test
  - publish

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  NODE_VERSION: "18.18"
  JAVA_VERSION: "17"

build:frontend:
  stage: build
  image: node:${NODE_VERSION}-alpine
  cache:
    key:
      files:
        - frontend/package-lock.json
    paths:
      - frontend/node_modules/
  script:
    - cd frontend
    - npm ci
    - npm run build
  artifacts:
    paths:
      - frontend/out/
    expire_in: 1 hour

build:jar:
  stage: build
  image: gradle:8.5-jdk17
  needs: ['build:frontend']
  cache:
    key:
      files:
        - gradle/wrapper/gradle-wrapper.properties
    paths:
      - .gradle/
  script:
    - ./gradlew :tracker-starter:copyFrontend --no-daemon
    - ./gradlew :tracker-starter:build --no-daemon
    - ./gradlew :tracker-starter:verifyJarContents --no-daemon
  artifacts:
    paths:
      - tracker-starter/build/libs/*.jar
    expire_in: 1 week

test:
  stage: test
  image: gradle:8.5-jdk17
  needs: ['build:jar']
  script:
    - ./gradlew test --no-daemon

publish:github:
  stage: publish
  image: gradle:8.5-jdk17
  needs: ['build:jar', 'test']
  only:
    - tags
  script:
    - ./gradlew :tracker-starter:publish -Pversion=${CI_COMMIT_TAG} --no-daemon
  variables:
    GITHUB_TOKEN: ${GITHUB_TOKEN}
    GITHUB_ACTOR: ${GITHUB_ACTOR}
```

---

## Jenkins Pipeline Example

### `Jenkinsfile`

```groovy
pipeline {
    agent any

    tools {
        nodejs 'NodeJS-18.18'
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Copy Frontend to Starter') {
            steps {
                sh './gradlew :tracker-starter:copyFrontend --no-daemon'
            }
        }

        stage('Build JAR') {
            steps {
                sh './gradlew :tracker-starter:build --no-daemon'
            }
        }

        stage('Verify JAR') {
            steps {
                sh './gradlew :tracker-starter:verifyJarContents --no-daemon'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
        }

        stage('Publish') {
            when {
                tag pattern: "v.*", comparator: "REGEXP"
            }
            steps {
                withCredentials([
                    string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN'),
                    string(credentialsId: 'github-actor', variable: 'GITHUB_ACTOR')
                ]) {
                    sh "./gradlew :tracker-starter:publish -Pversion=${TAG_NAME} --no-daemon"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'tracker-starter/build/libs/*.jar', allowEmptyArchive: true
            junit '**/build/test-results/test/*.xml'
        }
    }
}
```

---

## Key Requirements

### ✅ DO

1. **Install Node.js in CI** - Use Node 18.18+ for frontend builds
2. **Build frontend before starter** - `npm run build` → `copyFrontend` → `build`
3. **Verify JAR contents** - Use `verifyJarContents` task or manual `jar tf` check
4. **Use --no-daemon** - Prevents Gradle daemon issues in CI
5. **Cache dependencies** - Cache `node_modules/` and `.gradle/` for faster builds
6. **Fail fast on verification** - Exit with error if React dashboard is missing

### ❌ DON'T

1. **Don't skip frontend build** - JAR must always include React dashboard
2. **Don't commit build artifacts** - `frontend/out/`, `_next/`, etc. are never committed
3. **Don't assume Node.js locally** - Local builds work without Node (with warnings)
4. **Don't publish unverified JARs** - Always run verification before publish

---

## Environment Variables

### Required for Standalone UI (Optional)

If you want to deploy the frontend separately:

```yaml
- name: Build Standalone Frontend
  run: |
    cd frontend
    npm ci
    NEXT_PUBLIC_API_BASE_URL=https://api.example.com npm run build
```

The embedded dashboard (default) doesn't need this variable.

---

## Troubleshooting

### Issue: JAR verification fails in CI

**Symptom**: `verifyJarContents` shows missing React dashboard files

**Solution**:
1. Check that `npm run build` completed successfully
2. Verify `frontend/out/index.html` exists after build
3. Check `copyFrontend` task logs for errors
4. Ensure Node.js version is 18.18+

**Debug Commands**:
```bash
# Check if frontend built
ls -la frontend/out/

# Check if copied to starter
ls -la tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/

# Inspect JAR manually
jar tf tracker-starter/build/libs/tracker-starter.jar | grep aiprompt-tracker
```

---

### Issue: Node.js not found in CI

**Symptom**: `npm: command not found`

**Solution**: Add Node.js setup step before frontend build

**GitHub Actions**:
```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '18.18'
```

**GitLab CI**:
```yaml
image: node:18.18-alpine
```

---

### Issue: Tests fail with "dashboard-mvp.html not found"

**Symptom**: Backend smoke test expects UI but finds nothing

**Cause**: `copyFrontend` was not run, or resources not included in test classpath

**Solution**:
1. Ensure `copyFrontend` runs before `build`
2. Check that `dashboard-mvp.html` (vanilla fallback) exists in resources
3. This file should always be committed to git

---

## Summary

**Minimum CI Steps** (for Option A - embedded React):

```bash
# 1. Install Node.js (18.18+)
# 2. Install Java (17+)
cd frontend && npm ci && npm run build
cd .. && ./gradlew :tracker-starter:copyFrontend
./gradlew :tracker-starter:build
./gradlew :tracker-starter:verifyJarContents
# 3. Publish if verification passes
```

**Result**: Published JAR contains React dashboard, works without Node.js for consumers.
