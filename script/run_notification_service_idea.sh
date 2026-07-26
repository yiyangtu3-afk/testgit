#!/bin/zsh
set -e

export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR/notification-service"

for pid in $(lsof -tiTCP:8082 -sTCP:LISTEN 2>/dev/null); do
  command="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$command" == *"com.campuslink.notification.NotificationServiceApplication"* ]]; then
    echo "Stopping existing CampusLink notification service on port 8082 (PID $pid)..."
    kill "$pid"
    sleep 1
  fi
done

"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" spring-boot:run
