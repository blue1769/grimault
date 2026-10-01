#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Grimault Backend Deployment Script
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="${PROJECT_ROOT}/backend"

# 1. Load environment variables
ENV_FILE="${SCRIPT_DIR}/deploy.env"

if [[ ! -f "${ENV_FILE}" ]]; then
    if [[ -f "${SCRIPT_DIR}/.env" ]]; then
        ENV_FILE="${SCRIPT_DIR}/.env"
    else
        echo "Error: Configuration file not found in ${SCRIPT_DIR}" >&2
        echo "Please copy deploy.env.example to deploy.env and set the required variables." >&2
        exit 1
    fi
fi

set -a
source "${ENV_FILE}"
set +a

# Validate required configuration variables
for var in DEPLOY_USER DEPLOY_HOST DEPLOY_PATH; do
    if [[ -z "${!var:-}" ]]; then
        echo "Error: Required variable '${var}' is not defined in ${ENV_FILE}" >&2
        exit 1
    fi
done

# 2. Build Spring Boot JAR
echo "[1/3] Building Spring Boot application..."
cd "${BACKEND_DIR}"
./gradlew clean bootJar

JAR_FILE=$(ls build/libs/*.jar 2>/dev/null | grep -v 'plain' | head -n 1 || true)
if [[ -z "${JAR_FILE}" ]]; then
    echo "Error: Executable JAR file not found in build/libs/" >&2
    exit 1
fi
echo "Build successful: ${JAR_FILE}"

# 3. Transfer artifacts to NAS
echo "[2/3] Uploading JAR and Dockerfile to remote host..."
scp -O "${JAR_FILE}" "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/artifact-snapshot.jar"
scp -O "${BACKEND_DIR}/Dockerfile" "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/Dockerfile"

# 4. Rebuild container and restart
echo "[3/3] Rebuilding container and restarting service on remote host..."
ssh -t "${DEPLOY_USER}@${DEPLOY_HOST}" "cd ${DEPLOY_PATH} && sudo /usr/local/bin/docker compose up -d --build"

echo "Deployment complete. Streaming container logs (Press Ctrl+C to exit):"
ssh -t "${DEPLOY_USER}@${DEPLOY_HOST}" "sudo /usr/local/bin/docker logs -f grimault-api"
