#!/usr/bin/env bash
set -euo pipefail

KONG_ADMIN_URL="${KONG_ADMIN_URL:-http://localhost:8001}"

echo "Waiting for Kong Admin API at ${KONG_ADMIN_URL}..."
until curl -s -f "${KONG_ADMIN_URL}/status" > /dev/null 2>&1; do
  echo "Waiting for Kong to respond..."
  sleep 1
done

echo "Kong Admin API is online."

CONSUMER_NAME="harbor-test-client"
CUSTOM_ID="harbor-test-client-001"
CLIENT_ID="harbor-test-client-id"
CLIENT_SECRET="harbor-test-secret"

echo "Provisioning consumer: ${CONSUMER_NAME}..."
curl -s -o /dev/null -w "%{http_code}\n" -X POST "${KONG_ADMIN_URL}/consumers" \
  --data "username=${CONSUMER_NAME}" \
  --data "custom_id=${CUSTOM_ID}" || true

echo "Provisioning OAuth2 credentials for ${CONSUMER_NAME}..."
curl -s -o /dev/null -w "%{http_code}\n" -X POST "${KONG_ADMIN_URL}/consumers/${CONSUMER_NAME}/oauth2" \
  --data "name=HarborTestApp" \
  --data "client_id=${CLIENT_ID}" \
  --data "client_secret=${CLIENT_SECRET}" \
  --data "redirect_uris=http://localhost:8080/callback" || true

echo "Kong OAuth2 bootstrap completed successfully."
