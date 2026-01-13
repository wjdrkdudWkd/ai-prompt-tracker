# Gradle Frontend Integration Guide

## Overview

This document describes the production-grade Gradle build configuration for integrating the React (Next.js) frontend into the Spring Boot Starter JAR.

**Design Principles**:
1. **CI is the source of truth** - Frontend builds happen in CI, not locally
2. **Zero Node.js requirement for consumers** - Starter works without Node.js
3. **Clean separation** - Source code is versioned, build artifacts are not
4. **Graceful degradation** - Local builds work even without Node.js

---

## Architecture

### Source vs Build Artifacts

```
Version Controlled (Git):
├── frontend/                     # React source code ✅
│   ├── app/
│   ├── components/
│   ├── lib/
│   ├── package.json
│   └── next.config.js
└── tracker-starter/
    └── src/main/resources/META-INF/resources/aiprompt-tracker/
        └── dashboard-mvp.html    # Vanilla fallback only ✅

NOT Version Controlled (.gitignore):
├── frontend/out/                 # Next.js build output ❌
├── frontend/.next/               # Next.js cache ❌
└── tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/
    ├── index.html                # React build (from CI) ❌
    ├── _next/                    # JS/CSS bundles ❌
    ├── dashboard/                # Pre-rendered routes ❌
    └── functions/                # Pre-rendered routes ❌
```

**Key Point**: Only source code and the vanilla fallback (`dashboard-mvp.html`) are committed. All React build artifacts are generated in CI.

---

## Gradle Tasks

### 1. `buildFrontend` - Build Next.js Dashboard

**Purpose**: Build the React dashboard using `npm run build`

**Behavior**:
- ✅ If Node.js available: Runs `npm run build` in `frontend/`
- ⚠️ If Node.js unavailable: Skips with clear warning message

**Usage**:
```bash
# Build frontend manually
./gradlew :tracker-starter:buildFrontend
```

**Implementation**:
```kotlin
val buildFrontend by tasks.registering(Exec::class) {
    val nodeAvailable = isNodeAvailable()

    if (nodeAvailable) {
        commandLine("npm", "run", "build")
    } else {
        commandLine("echo", "Skipping frontend build - Node.js not available")
        // Shows warning message
    }
}
```

**Node.js Detection** (Type-Safe):
```kotlin
import java.util.concurrent.TimeUnit

fun isNodeAvailable(): Boolean {
    return try {
        val process = ProcessBuilder("node", "--version")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(5, TimeUnit.SECONDS)  // ✅ Use imported TimeUnit
        process.exitValue() == 0
    } catch (e: Exception) {
        false
    }
}
```

**Why This Works**:
- ✅ No `exec { }.exitValue` type errors
- ✅ Uses `ProcessBuilder` directly (Java API)
- ✅ Graceful timeout (5 seconds)
- ✅ Returns boolean (type-safe)

---

### 2. `copyFrontend` - Copy Build Output to Starter

**Purpose**: Copy `frontend/out/` to starter resources

**Behavior**:
- ✅ If `frontend/out/index.html` exists: Copies files
- ⚠️ If output missing: Skips with warning message
- ✅ Preserves `dashboard-mvp.html` (vanilla fallback)

**Usage**:
```bash
# Build + Copy in one command
./gradlew :tracker-starter:copyFrontend

# This automatically runs buildFrontend first
```

**Implementation**:
```kotlin
val copyFrontend by tasks.registering(Copy::class) {
    dependsOn(buildFrontend)

    val outDir = file("../frontend/out")
    val targetDir = file("src/main/resources/META-INF/resources/aiprompt-tracker")

    // Only copy if build output exists
    onlyIf {
        hasFrontendBuildOutput()
    }

    from(outDir) {
        include("**/*")
        exclude("dashboard-mvp.html") // Preserve the one in resources
    }
    into(targetDir)

    // Preserve dashboard-mvp.html during copy
    doFirst { /* backup */ }
    doLast { /* restore */ }
}
```

---

## Build Flows

### Local Development (Without Node.js)

```bash
# Build starter without frontend
./gradlew :tracker-starter:build

# Output:
# ⚠️  Node.js not found - skipping frontend build
#    React dashboard will not be included in this build
# ✅ BUILD SUCCESSFUL
```

**Result**: Starter JAR is built successfully, but without React dashboard (only vanilla fallback).

---

### Local Development (With Node.js)

```bash
# Build frontend + copy + build starter
./gradlew :tracker-starter:copyFrontend
./gradlew :tracker-starter:build

# Or in one command:
./gradlew :tracker-starter:copyFrontend :tracker-starter:build
```

**Result**: Starter JAR includes React dashboard.

---

### CI Pipeline (Production)

```yaml
# .github/workflows/build.yml
steps:
  - name: Setup Node.js
    uses: actions/setup-node@v3
    with:
      node-version: '18.18'

  - name: Setup Java
    uses: actions/setup-java@v3
    with:
      java-version: '17'

  - name: Build Frontend
    run: |
      cd frontend
      npm ci
      npm run build

  - name: Copy Frontend to Starter
    run: ./gradlew :tracker-starter:copyFrontend

  - name: Build Starter JAR
    run: ./gradlew :tracker-starter:build

  - name: Publish
    run: ./gradlew :tracker-starter:publish
```

**Result**: Published starter JAR includes React dashboard.

---

## Key Design Decisions

### 1. `copy-to-starter.sh` - Keep or Remove?

**Decision**: **Keep as convenience script, but Gradle is primary**

**Rationale**:
- Bash script is simpler for manual testing
- Gradle task is idiomatic for CI
- Both serve the same purpose

**Recommendation**:
```bash
# Local quick testing
./frontend/copy-to-starter.sh

# CI and production builds
./gradlew :tracker-starter:copyFrontend
```

---

### 2. `processResources` Dependency - Enable or Disable?

**Current State**: **Disabled by default (commented out)**

```kotlin
// tasks.named("processResources") {
//     dependsOn(copyFrontend)
// }
```

**Rationale**:
- **Local development**: Frontend build is slow and optional
- **CI**: Frontend is built explicitly before starter build
- **Consumers**: Never need to build frontend (it's already in JAR)

**When to Enable**:
- If you want every `./gradlew build` to include frontend
- If your CI doesn't run separate frontend build steps

**Recommendation**: Keep disabled for local development, enable in CI-specific profiles if needed.

---

### 3. gitignore Strategy - What to Exclude?

**Excluded (Build Artifacts)**:
```gitignore
# Frontend build outputs
frontend/out/
frontend/.next/
frontend/.eslintcache

# Copied build artifacts in starter
tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/*.html
tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/_next/
tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/dashboard/
tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/functions/
# ... other routes

# Exception: Keep vanilla fallback
!tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/dashboard-mvp.html
```

**Included (Source Code)**:
```
✅ frontend/app/
✅ frontend/components/
✅ frontend/lib/
✅ frontend/package.json
✅ frontend/next.config.js
✅ tracker-starter/src/main/resources/.../dashboard-mvp.html (fallback only)
```

**Why This Works**:
- Source code is versioned
- Build artifacts are generated in CI
- Consumers never see build artifacts in Git
- Starter JAR contains embedded dashboard (from CI build)

---

## Troubleshooting

### Issue 1: Kotlin DSL Type Error on `exitValue`

**Error**:
```
Val cannot be reassigned
Type mismatch: inferred type is ExecResult but Int was expected
```

**Cause**: Using `exec { }.exitValue` directly in task configuration

**Solution**: Use `ProcessBuilder` in a helper function
```kotlin
fun isNodeAvailable(): Boolean {
    return try {
        val process = ProcessBuilder("node", "--version")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        process.exitValue() == 0
    } catch (e: Exception) {
        false
    }
}
```

---

### Issue 2: Frontend Build Artifacts Committed to Git

**Problem**: `frontend/out/` or copied files in `tracker-starter/src/main/resources/` are committed

**Solution**:
```bash
# Remove from Git
git rm -r --cached frontend/out
git rm -r --cached tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/_next
git rm -r --cached tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/dashboard
# ... etc

# Commit removal
git commit -m "chore: remove frontend build artifacts from Git"

# Verify .gitignore is correct
git status  # Should not show build artifacts
```

---

### Issue 3: CI Build Fails Without Frontend

**Problem**: CI tries to build starter but frontend is missing

**Solution**: Ensure CI explicitly builds frontend first
```yaml
# BEFORE starter build
- name: Build Frontend
  run: |
    cd frontend
    npm ci
    npm run build

# Copy to starter
- name: Copy Frontend
  run: ./gradlew :tracker-starter:copyFrontend

# NOW build starter
- name: Build Starter
  run: ./gradlew :tracker-starter:build
```

---

### Issue 4: Local Build Includes Stale Frontend

**Problem**: Old frontend build is embedded in JAR

**Solution**: Clean before building
```bash
# Remove old build artifacts
rm -rf frontend/out
rm -rf tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/_next
rm -rf tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/dashboard
# ... or use clean task

# Build fresh
./gradlew :tracker-starter:copyFrontend :tracker-starter:build
```

---

## Best Practices Summary

### ✅ DO

1. **Commit only source code** - Never commit `frontend/out/` or copied artifacts
2. **Build frontend in CI** - Make CI the authoritative source for frontend builds
3. **Use Gradle tasks** - Prefer `./gradlew copyFrontend` over bash scripts in automation
4. **Test without Node.js** - Ensure builds work even when Node.js is missing
5. **Keep vanilla fallback** - `dashboard-mvp.html` is the only HTML in Git

### ❌ DON'T

1. **Don't commit build artifacts** - `frontend/out/`, `_next/`, etc. are never committed
2. **Don't require Node.js for consumers** - Starter must work without Node.js installed
3. **Don't enable `processResources` dependency by default** - Keep builds fast locally
4. **Don't use fragile Node.js detection** - Use `ProcessBuilder`, not `exec { }.exitValue`
5. **Don't couple frontend and starter builds** - They can be built independently

---

## Migration Checklist

If you're cleaning up an existing repository:

- [ ] Remove frontend build artifacts from Git
  ```bash
  git rm -r --cached frontend/out
  git rm -r --cached tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/_next
  git rm -r --cached tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/dashboard
  git rm -r --cached tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/functions
  # Keep dashboard-mvp.html
  ```

- [ ] Update `.gitignore` with patterns from this guide

- [ ] Update `build.gradle.kts` with type-safe Node.js detection

- [ ] Update CI to explicitly build frontend before starter

- [ ] Document build process in README

- [ ] Test local build without Node.js (should succeed)

- [ ] Test local build with Node.js (should include dashboard)

- [ ] Test CI build (should include dashboard in published JAR)

---

## Summary

**Key Points**:
- ✅ Source code is versioned, build artifacts are not
- ✅ CI builds and embeds frontend into starter JAR
- ✅ Local builds work without Node.js (graceful degradation)
- ✅ Type-safe Gradle Kotlin DSL (no IDE errors)
- ✅ Clean separation of concerns

**Build Commands**:
```bash
# Local (with Node.js)
./gradlew :tracker-starter:copyFrontend :tracker-starter:build

# Local (without Node.js)
./gradlew :tracker-starter:build  # Skips frontend gracefully

# CI
npm ci && npm run build  # In frontend/
./gradlew :tracker-starter:copyFrontend :tracker-starter:build
```

**Result**: Production-grade, maintainable, and CI-friendly frontend integration.
