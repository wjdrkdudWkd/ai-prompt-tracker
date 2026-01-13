# Publishing Verification Guide

This guide helps verify that the `ai-prompt-tracker-starter` publishes correctly with proper Java 17 bytecode and metadata that works reliably with Java 21+ consumer projects.

## Build Configuration Summary

### Java Compilation
- **Toolchain**: Java 17 (explicit)
- **Source/Target**: Java 17
- **Compiler Flag**: `--release 17` (ensures correct bytecode and API usage)

### Published Artifacts
1. **Main JAR**: `ai-prompt-tracker-starter-<version>.jar`
2. **Sources JAR**: `ai-prompt-tracker-starter-<version>-sources.jar`
3. **Javadoc JAR**: `ai-prompt-tracker-starter-<version>-javadoc.jar`
4. **POM**: `ai-prompt-tracker-starter-<version>.pom`
5. **Gradle Module Metadata**: `ai-prompt-tracker-starter-<version>.module` (optional)

### Key Changes
- ✅ Spring Boot plugin **NOT** applied to tracker-starter (library module)
- ✅ Only `java-library` + `dependency-management` plugins used
- ✅ Explicit Java 17 toolchain configuration
- ✅ `--release 17` flag for correct bytecode generation
- ✅ Javadoc jar generation enabled
- ✅ Gradle module metadata configured for cross-version compatibility

---

## Local Publishing & Verification

### Step 1: Publish to Local Maven Repository

```bash
# Clean build and publish to ~/.m2/repository
./gradlew clean :tracker-starter:publishToMavenLocal -Pversion=1.0.0-TEST

# Or publish all modules
./gradlew clean publishToMavenLocal -Pversion=1.0.0-TEST
```

**Expected Output:**
```
> Task :tracker-starter:printVersion
📦 Building: com.galoong:tracker-starter:1.0.0-TEST

BUILD SUCCESSFUL
```

### Step 2: Verify Published Files

```bash
# Navigate to local Maven repository
cd ~/.m2/repository/com/galoong/ai-prompt-tracker-starter/1.0.0-TEST/

# List all published files
ls -lh
```

**Expected Files:**
```
ai-prompt-tracker-starter-1.0.0-TEST.jar
ai-prompt-tracker-starter-1.0.0-TEST-sources.jar
ai-prompt-tracker-starter-1.0.0-TEST-javadoc.jar
ai-prompt-tracker-starter-1.0.0-TEST.pom
ai-prompt-tracker-starter-1.0.0-TEST.module (if Gradle metadata enabled)
```

### Step 3: Verify Java Bytecode Version

```bash
# Extract and check class file bytecode version
cd ~/.m2/repository/com/galoong/ai-prompt-tracker-starter/1.0.0-TEST/
unzip -q ai-prompt-tracker-starter-1.0.0-TEST.jar
javap -v com/galoong/aiprompttracker/config/TrackingAutoConfiguration.class | grep "major version"
```

**Expected Output:**
```
major version: 61
```

**Bytecode Version Reference:**
- Java 17 = major version 61 ✅
- Java 21 = major version 65
- Java 11 = major version 55

### Step 4: Verify POM Metadata

```bash
# View POM content
cat ~/.m2/repository/com/galoong/ai-prompt-tracker-starter/1.0.0-TEST/ai-prompt-tracker-starter-1.0.0-TEST.pom
```

**Check for:**
- ✅ Correct `<groupId>com.galoong</groupId>`
- ✅ Correct `<artifactId>ai-prompt-tracker-starter</artifactId>`
- ✅ Correct `<version>1.0.0-TEST</version>`
- ✅ Spring Boot dependency versions resolved (not BOM references)
- ✅ No `<parent>` tag (should be standalone)

### Step 5: Verify Gradle Module Metadata (Optional)

```bash
# View module metadata (if enabled)
cat ~/.m2/repository/com/galoong/ai-prompt-tracker-starter/1.0.0-TEST/ai-prompt-tracker-starter-1.0.0-TEST.module
```

**Check for:**
- ✅ `"org.gradle.jvm.version": 17`
- ✅ Variants: `apiElements`, `runtimeElements`
- ✅ No Spring Boot plugin artifacts in capabilities

---

## Consumer Project Testing

### Step 1: Add Dependency to Consumer

In your consumer project (e.g., using Java 21 toolchain):

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.galoong:ai-prompt-tracker-starter:1.0.0-TEST")
}

repositories {
    mavenLocal()
    mavenCentral()
}
```

### Step 2: Force Refresh Gradle Cache

```bash
# Force refresh dependencies (clears cached metadata)
./gradlew clean build --refresh-dependencies

# Or completely clear Gradle cache
rm -rf ~/.gradle/caches/modules-2/files-2.1/com.galoong/ai-prompt-tracker-starter/
./gradlew clean build
```

### Step 3: Check Dependency Resolution

```bash
# View dependency resolution details
./gradlew :your-module:dependencies --configuration runtimeClasspath | grep ai-prompt-tracker-starter

# Detailed dependency insight
./gradlew :your-module:dependencyInsight --dependency ai-prompt-tracker-starter --configuration runtimeClasspath
```

**Expected Output (GOOD):**
```
com.galoong:ai-prompt-tracker-starter:1.0.0-TEST
   variant "runtimeElements" [
      org.gradle.status = release
      org.gradle.usage  = java-runtime
      org.gradle.jvm.version = 17 (compatible with 21)
   ]
```

**Warning Signs (BAD):**
```
❌ "selected by rule" appearing
❌ "requested jvm.version=21 but provided=17" conflicts
❌ Multiple SNAPSHOT jars cached for same version
```

### Step 4: Verify IDE Import

1. Refresh Gradle in your IDE (IntelliJ: Gradle tool window → Reload)
2. Try importing a class from the starter:
   ```java
   import com.galoong.aiprompttracker.core.annotation.AIPrompt;
   ```
3. IDE should auto-complete and resolve the class

**If IDE can't find classes but build works:**
- Invalidate IDE caches (IntelliJ: File → Invalidate Caches)
- Re-import Gradle project
- Check IDE is using correct JDK (should match toolchain)

### Step 5: Run Consumer Application

```bash
# Start consumer application
./gradlew bootRun

# Check logs for starter initialization
```

**Expected Logs:**
```
AI Prompt Tracker: Core auto-configuration loaded
AI Prompt Tracker: JPA entities and repositories auto-registered
AI Prompt Tracker: API auto-configuration loaded
```

---

## GitHub Packages Publishing

### Publish to GitHub Packages

```bash
# Set GitHub credentials
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-github-personal-access-token

# Publish release version
./gradlew publish -Pversion=1.2.3

# Publish snapshot version
./gradlew publish -Pversion=1.2.4-SNAPSHOT
```

### Verify GitHub Packages

1. Go to: `https://github.com/wjdrkdudWkd/ai-prompt-tracker/packages`
2. Check version is listed
3. Download artifacts to verify

---

## Troubleshooting

### Issue: "selected by rule" in dependency resolution

**Cause:** Gradle module metadata variant mismatch

**Solutions:**
1. Disable Gradle module metadata (uncomment in `tracker-starter/build.gradle.kts`):
   ```kotlin
   tasks.withType<GenerateModuleMetadata> {
       enabled = false
   }
   ```
2. Republish and test

### Issue: IDE can't import classes but build works

**Cause:** IDE cache or metadata issues

**Solutions:**
1. Force refresh consumer dependencies: `./gradlew --refresh-dependencies`
2. Clear Gradle cache: `rm -rf ~/.gradle/caches/`
3. Invalidate IDE caches
4. Re-import Gradle project

### Issue: Multiple cached SNAPSHOT jars

**Cause:** Gradle caches SNAPSHOT versions with timestamps

**Solutions:**
1. Always use `--refresh-dependencies` when testing SNAPSHOTs
2. Or manually delete: `rm -rf ~/.gradle/caches/modules-2/files-2.1/com.galoong/ai-prompt-tracker-starter/`
3. Consider using timestamped test versions: `1.0.0-TEST1`, `1.0.0-TEST2`

### Issue: Wrong bytecode version (major version 65 instead of 61)

**Cause:** `--release` flag not applied or wrong toolchain

**Solutions:**
1. Verify `tracker-starter/build.gradle.kts` has:
   ```kotlin
   options.release.set(17)
   ```
2. Check toolchain is Java 17: `./gradlew -q javaToolchains`
3. Rebuild and republish

### Issue: Spring Boot bootJar task not found

**Cause:** Root build.gradle.kts still applies Spring Boot plugin to tracker-starter

**Solutions:**
1. Verify root `build.gradle.kts` has exclusion:
   ```kotlin
   if (project.name != "tracker-starter") {
       apply(plugin = "org.springframework.boot")
   }
   ```
2. Clean and rebuild: `./gradlew clean build`

---

## Checklist for Each Release

Before publishing a new version:

- [ ] Version number set correctly (via `-Pversion=X.Y.Z`)
- [ ] Clean build passes: `./gradlew clean build`
- [ ] Bytecode version verified (major version 61)
- [ ] Publish to Maven Local works
- [ ] All 3 JARs generated (main, sources, javadoc)
- [ ] POM metadata looks correct
- [ ] Test in consumer project with `mavenLocal()`
- [ ] Consumer dependency resolution clean (no "selected by rule")
- [ ] IDE can import classes
- [ ] GitHub credentials set (for GitHub Packages)
- [ ] Publish to GitHub Packages: `./gradlew publish -Pversion=X.Y.Z`
- [ ] Verify on GitHub Packages UI

---

## Quick Commands Reference

```bash
# Local build and test
./gradlew clean build

# Publish to Maven Local
./gradlew publishToMavenLocal -Pversion=1.0.0-TEST

# Check bytecode version
javap -v <class-file> | grep "major version"

# Force refresh consumer dependencies
./gradlew --refresh-dependencies

# Clear Gradle cache
rm -rf ~/.gradle/caches/

# Publish to GitHub Packages
./gradlew publish -Pversion=1.2.3

# Check Java toolchains
./gradlew -q javaToolchains

# Dependency insight
./gradlew dependencyInsight --dependency ai-prompt-tracker-starter --configuration runtimeClasspath
```
