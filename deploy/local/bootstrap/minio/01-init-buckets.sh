#!/bin/sh
set -e

echo "Configuring MinIO client alias..."
until mc alias set local http://minio:9000 harbor_admin harbor_password > /dev/null 2>&1; do
  echo "Waiting for MinIO to be ready..."
  sleep 1
done

echo "Creating default buckets..."
mc mb --ignore-existing local/harbor-quarantine
mc mb --ignore-existing local/harbor-admitted

echo "MinIO buckets initialized successfully."
