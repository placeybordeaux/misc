#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# build.sh - Build the Color App Picker Android app
# ============================================================
#
# Prerequisites:
#   - Java 17+ (JAVA_HOME set)
#   - Android SDK installed (ANDROID_HOME / ANDROID_SDK_ROOT set)
#
# Usage:
#   ./build.sh              # Debug build
#   ./build.sh release      # Release build
#   ./build.sh clean        # Clean build artifacts
#   ./build.sh install      # Build + install on connected device/emulator
# ============================================================

cd "$(dirname "$0")"

# --- Check prerequisites ---
check_prereqs() {
    if ! command -v java &>/dev/null; then
        echo "ERROR: java not found. Install JDK 17+ and set JAVA_HOME."
        exit 1
    fi

    java_version=$(java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\).*/\1/')
    if [[ "$java_version" -lt 17 ]]; then
        echo "ERROR: Java 17+ required (found version $java_version)."
        exit 1
    fi

    if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
        echo "ERROR: ANDROID_HOME or ANDROID_SDK_ROOT not set."
        echo "  Install Android SDK and set the environment variable."
        echo "  Common paths:"
        echo "    macOS:   ~/Library/Android/sdk"
        echo "    Linux:   ~/Android/Sdk"
        echo "    Windows: %LOCALAPPDATA%/Android/Sdk"
        exit 1
    fi

    echo "OK: Java $(java -version 2>&1 | head -1)"
    echo "OK: ANDROID_HOME=${ANDROID_HOME:-${ANDROID_SDK_ROOT}}"
}

# --- Ensure Gradle wrapper exists ---
ensure_gradlew() {
    if [[ ! -x ./gradlew ]]; then
        echo "Generating Gradle wrapper..."
        if command -v gradle &>/dev/null; then
            gradle wrapper --gradle-version 8.2
        else
            echo "ERROR: No gradlew found and 'gradle' not installed."
            echo "  Install Gradle or download the wrapper:"
            echo "  https://docs.gradle.org/current/userguide/gradle_wrapper.html"
            exit 1
        fi
    fi
}

# --- Main ---
check_prereqs
ensure_gradlew

case "${1:-debug}" in
    clean)
        echo ""
        echo "=== Cleaning ==="
        ./gradlew clean
        echo "Done."
        ;;
    release)
        echo ""
        echo "=== Building Release APK ==="
        ./gradlew assembleRelease
        echo ""
        echo "APK: app/build/outputs/apk/release/app-release-unsigned.apk"
        ;;
    install)
        echo ""
        echo "=== Building & Installing Debug APK ==="
        ./gradlew installDebug
        echo ""
        echo "Installed on connected device. Open 'Color Picker' from your app drawer."
        ;;
    debug|*)
        echo ""
        echo "=== Building Debug APK ==="
        ./gradlew assembleDebug
        echo ""
        echo "APK: app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "To install on a device:  ./build.sh install"
        echo "To build release:        ./build.sh release"
        ;;
esac
