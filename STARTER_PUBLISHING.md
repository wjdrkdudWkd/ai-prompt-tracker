# AI Prompt Tracker Starter - Publishing Guide

This document explains how the `ai-prompt-tracker-starter` module is published to GitHub Packages and how to consume it in your projects.

## 📦 Published Artifacts

**Group ID**: `com.galoong`
**Artifact ID**: `ai-prompt-tracker-starter`
**Repository**: GitHub Packages Maven Registry

## 🔄 Versioning Strategy

### Release Versions
- **Trigger**: Push a Git tag matching `v*.*.*` (e.g., `v0.1.0`, `v1.2.3`)
- **Version**: Extracted from tag (e.g., tag `v0.1.0` → version `0.1.0`)
- **Behavior**:
  - Publishes a release version (no `-SNAPSHOT` suffix)
  - Creates a GitHub Release with auto-generated changelog
  - Suitable for production use

**Example**:
```bash
git tag v0.1.0
git push origin v0.1.0
# → Publishes version 0.1.0
```

### Snapshot Versions
- **Trigger**: Push to `main` branch
- **Version**: `<latest-tag-version>-SNAPSHOT` (e.g., `0.1.0-SNAPSHOT`)
- **Behavior**:
  - Publishes a snapshot version for testing
  - No GitHub Release created
  - Suitable for development/testing

**Example**:
```bash
git push origin main
# → Publishes version 0.1.0-SNAPSHOT (based on latest tag v0.1.0)
```

If no tags exist, publishes as `0.0.0-SNAPSHOT`.

## 🚀 How to Consume

### Prerequisites

GitHub Packages requires authentication even for public packages. You need a GitHub Personal Access Token (PAT) with `read:packages` permission.

#### Generate a Token
1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select scopes: `read:packages` (minimum required)
4. Generate and copy the token

### Gradle (Kotlin DSL)

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/wjdrkdudWkd/ai-prompt-tracker")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.galoong:ai-prompt-tracker-starter:0.1.0")
}
```

### Gradle (Groovy DSL)

Add to your `build.gradle`:

```groovy
repositories {
    mavenCentral()

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/wjdrkdudWkd/ai-prompt-tracker")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.galoong:ai-prompt-tracker-starter:0.1.0'
}
```

### Maven

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/wjdrkdudWkd/ai-prompt-tracker</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.galoong</groupId>
        <artifactId>ai-prompt-tracker-starter</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

Add to your `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

## 🔐 Authentication Setup

### Option 1: Environment Variables (Recommended for CI/CD)

```bash
export GITHUB_ACTOR="your-github-username"
export GITHUB_TOKEN="ghp_your_token_here"
```

### Option 2: Gradle Properties (Recommended for Local Development)

Create or edit `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.key=ghp_your_token_here
```

**Security**: This file is in your home directory and NOT committed to version control.

### Option 3: Project Properties (Not Recommended)

You can also add to your project's `gradle.properties`, but **DO NOT commit tokens to Git**:

```properties
gpr.user=your-github-username
gpr.key=ghp_your_token_here
```

Add `gradle.properties` to `.gitignore` if using this approach.

## 📋 Available Versions

View all published versions at:
https://github.com/wjdrkdudWkd/ai-prompt-tracker/packages

## 🛠️ For Maintainers

### Publishing a Release

1. Ensure `main` branch is clean and tests pass
2. Create and push a version tag:
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```
3. GitHub Actions will:
   - Run tests
   - Build the artifact
   - Publish to GitHub Packages with version `0.1.0`
   - Create a GitHub Release with changelog

### Publishing a Snapshot

Simply push to `main`:
```bash
git push origin main
```

GitHub Actions will publish a snapshot version based on the latest tag.

### Manual Publishing (Local Testing)

To test publishing locally without actually publishing:

```bash
# Test publication configuration
./gradlew :tracker-starter:publishToMavenLocal

# Check output in ~/.m2/repository/com/galoong/ai-prompt-tracker-starter/
```

To publish to GitHub Packages manually (requires `GITHUB_TOKEN`):

```bash
export GITHUB_ACTOR="your-username"
export GITHUB_TOKEN="your-token"

./gradlew :tracker-starter:publishMavenPublicationToGitHubPackagesRepository \
  -Pversion=0.1.0
```

## ❓ Troubleshooting

### Error: "Could not resolve com.galoong:ai-prompt-tracker-starter:X.Y.Z"

**Cause**: Missing or invalid GitHub authentication.

**Solution**:
1. Verify your GitHub token has `read:packages` permission
2. Check credentials are correctly set in environment variables or `gradle.properties`
3. Ensure the version exists in GitHub Packages

### Error: "401 Unauthorized"

**Cause**: Invalid or expired GitHub token.

**Solution**: Generate a new Personal Access Token with `read:packages` scope.

### Error: "404 Not Found"

**Cause**: Package or version doesn't exist yet.

**Solution**:
1. Check available versions at: https://github.com/wjdrkdudWkd/ai-prompt-tracker/packages
2. Ensure the version was successfully published
3. Wait a few minutes after publishing (propagation delay)

## 📚 Additional Resources

- [GitHub Packages Documentation](https://docs.github.com/en/packages)
- [Working with Gradle Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
- [Project README](./README.md)
