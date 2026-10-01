#!/bin/sh
set -e

export VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-harbor-vault-root-token}"

echo "Initializing secrets in Vault at ${VAULT_ADDR}..."

# Wait for Vault to be ready
until vault status > /dev/null 2>&1 || curl -s -f "${VAULT_ADDR}/v1/sys/health" > /dev/null 2>&1; do
  echo "Waiting for Vault to be ready..."
  sleep 1
done

# Pre-seed secrets using vault CLI if available, otherwise curl REST API
if command -v vault > /dev/null 2>&1; then
  echo "Seeding secrets using vault CLI..."
  vault kv put secret/berths/inbound-sftp username="vendor_push" password="dock_password"
  vault kv put secret/berths/titan-remote-sftp username="titan_client" private_key="mock_ssh_key"
  vault kv put secret/storage/s3 endpoint="http://localstack:4566" access_key="test" secret_key="test" region="us-east-1"
else
  echo "Seeding secrets using REST API..."
  curl -s --fail --header "X-Vault-Token: ${VAULT_TOKEN}" \
       --request POST \
       --data '{"data": {"username": "vendor_push", "password": "dock_password"}}' \
       "${VAULT_ADDR}/v1/secret/data/berths/inbound-sftp"

  curl -s --fail --header "X-Vault-Token: ${VAULT_TOKEN}" \
       --request POST \
       --data '{"data": {"username": "titan_client", "private_key": "mock_ssh_key"}}' \
       "${VAULT_ADDR}/v1/secret/data/berths/titan-remote-sftp"

  curl -s --fail --header "X-Vault-Token: ${VAULT_TOKEN}" \
       --request POST \
       --data '{"data": {"endpoint": "http://localstack:4566", "access_key": "test", "secret_key": "test", "region": "us-east-1"}}' \
       "${VAULT_ADDR}/v1/secret/data/storage/s3"
fi

echo "Vault secrets initialized successfully."
