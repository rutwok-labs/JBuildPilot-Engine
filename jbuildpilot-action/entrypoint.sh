#!/bin/bash
set -e

MODE=$1
TASK_ID=$2
# GITHUB_TOKEN is passed as $3 but we should not log it or expose it insecurely.
export GITHUB_TOKEN="$3"

echo "🛡️ Starting JBuildPilot Repository Agent Mode"
echo "Mode: $MODE"

# If running inside the JBuildPilot monorepo (dog-fooding), the JAR might exist.
# Otherwise, for external developers, we download the latest published CLI release.
JAR_PATH="/opt/jbuildpilot-cli.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Downloading latest JBuildPilot CLI from GitHub Releases..."
    curl -sL "https://github.com/rutwok-labs/JBuildPilot-Engine/releases/latest/download/jbuildpilot-cli.jar" -o "$JAR_PATH"
fi

if [ "$MODE" = "diagnose" ]; then
    echo "Executing: pilot project diagnose"
    java -jar "$JAR_PATH" project diagnose
    echo "Diagnosis complete."
elif [ "$MODE" = "apply" ]; then
    if [ -n "$TASK_ID" ]; then
        echo "Executing: pilot project apply $TASK_ID"
        java -jar "$JAR_PATH" project apply "$TASK_ID"
    else
        echo "Executing: pilot project apply"
        java -jar "$JAR_PATH" project apply
    fi
    echo "Task applied successfully."
else
    echo "Unknown mode: $MODE"
    exit 1
fi
