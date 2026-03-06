#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# test.sh - Run tests for the Color App Picker
# ============================================================
#
# Prerequisites:
#   - Java 17+ (JAVA_HOME set)
#   - Android SDK installed (ANDROID_HOME / ANDROID_SDK_ROOT set)
#
# Usage:
#   ./test.sh                # Run all unit tests
#   ./test.sh --verbose      # Run with verbose output
#   ./test.sh --class NAME   # Run a specific test class
# ============================================================

cd "$(dirname "$0")"

VERBOSE=""
CLASS=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --verbose|-v)
            VERBOSE="--info"
            shift
            ;;
        --class|-c)
            CLASS="--tests $2"
            shift 2
            ;;
        *)
            echo "Usage: $0 [--verbose] [--class TestClassName]"
            exit 1
            ;;
    esac
done

# --- Check prerequisites ---
if ! command -v java &>/dev/null; then
    echo "ERROR: java not found. Install JDK 17+ and set JAVA_HOME."
    exit 1
fi

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "ERROR: ANDROID_HOME or ANDROID_SDK_ROOT not set."
    exit 1
fi

# --- Ensure Gradle wrapper ---
if [[ ! -x ./gradlew ]]; then
    if command -v gradle &>/dev/null; then
        gradle wrapper --gradle-version 8.2
    else
        echo "ERROR: No gradlew found and 'gradle' not installed."
        exit 1
    fi
fi

echo "=== Running Unit Tests ==="
echo ""

# Run local unit tests (JVM-based, no device needed)
# Robolectric provides Android framework stubs
./gradlew testDebugUnitTest $VERBOSE $CLASS

echo ""
echo "=== Test Results ==="
REPORT="app/build/reports/tests/testDebugUnitTest/index.html"
if [[ -f "$REPORT" ]]; then
    echo "HTML report: $REPORT"
    # Try to open in browser
    if command -v open &>/dev/null; then
        open "$REPORT" 2>/dev/null || true
    elif command -v xdg-open &>/dev/null; then
        xdg-open "$REPORT" 2>/dev/null || true
    fi
fi

# Print summary from XML results
XML_DIR="app/build/test-results/testDebugUnitTest"
if [[ -d "$XML_DIR" ]]; then
    TOTAL=0
    PASSED=0
    FAILED=0
    for xml in "$XML_DIR"/*.xml; do
        [[ -f "$xml" ]] || continue
        tests=$(grep -oP 'tests="\K[0-9]+' "$xml" | head -1 || echo 0)
        failures=$(grep -oP 'failures="\K[0-9]+' "$xml" | head -1 || echo 0)
        errors=$(grep -oP 'errors="\K[0-9]+' "$xml" | head -1 || echo 0)
        TOTAL=$((TOTAL + tests))
        FAILED=$((FAILED + failures + errors))
    done
    PASSED=$((TOTAL - FAILED))
    echo ""
    echo "Total: $TOTAL | Passed: $PASSED | Failed: $FAILED"
    if [[ $FAILED -gt 0 ]]; then
        echo ""
        echo "TESTS FAILED"
        exit 1
    else
        echo ""
        echo "ALL TESTS PASSED"
    fi
fi
