#!/bin/bash
#
# Copy Next.js build output to Spring Boot starter resources
#
# Usage: ./copy-to-starter.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

FRONTEND_OUT="$SCRIPT_DIR/out"
STARTER_RESOURCES="$PROJECT_ROOT/tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker"

echo "════════════════════════════════════════════════════════════"
echo "  Copying React Dashboard to Starter Resources"
echo "════════════════════════════════════════════════════════════"
echo ""

# Check if out directory exists
if [ ! -d "$FRONTEND_OUT" ]; then
    echo "❌ Error: Build output directory not found: $FRONTEND_OUT"
    echo ""
    echo "Please run 'npm run build' first:"
    echo "  cd frontend"
    echo "  npm install"
    echo "  npm run build"
    echo ""
    exit 1
fi

# Check if out directory has content
if [ ! -f "$FRONTEND_OUT/index.html" ]; then
    echo "❌ Error: Build output appears incomplete (no index.html)"
    echo ""
    echo "Please rebuild:"
    echo "  cd frontend"
    echo "  npm run build"
    echo ""
    exit 1
fi

# Create target directory if it doesn't exist
mkdir -p "$STARTER_RESOURCES"

# Backup dashboard.html (MVP fallback) if it exists
if [ -f "$STARTER_RESOURCES/dashboard.html" ]; then
    echo "📦 Backing up dashboard.html (MVP fallback)..."
    cp "$STARTER_RESOURCES/dashboard.html" "$STARTER_RESOURCES/dashboard-mvp.html.backup"
fi

# Remove old React build artifacts (but keep dashboard.html backup)
echo "🗑️  Cleaning old React build artifacts..."
find "$STARTER_RESOURCES" -mindepth 1 ! -name 'dashboard-mvp.html.backup' -delete

# Copy new build
echo "📋 Copying Next.js build output..."
cp -r "$FRONTEND_OUT"/* "$STARTER_RESOURCES/"

# Restore dashboard.html as fallback
if [ -f "$STARTER_RESOURCES/dashboard-mvp.html.backup" ]; then
    echo "♻️  Restoring dashboard.html as fallback..."
    mv "$STARTER_RESOURCES/dashboard-mvp.html.backup" "$STARTER_RESOURCES/dashboard-mvp.html"
fi

echo ""
echo "✅ Copy complete!"
echo ""
echo "Copied files:"
ls -lh "$STARTER_RESOURCES" | head -20
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  Next steps:"
echo "  1. Build starter: ./gradlew :tracker-starter:build"
echo "  2. Run app and access: http://localhost:8080/aiprompt-tracker/"
echo "════════════════════════════════════════════════════════════"
