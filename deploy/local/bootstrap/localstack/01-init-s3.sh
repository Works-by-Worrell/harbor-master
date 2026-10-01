#!/bin/sh
echo "Initializing Harbor Master S3 buckets in LocalStack..."
awslocal s3 mb s3://harbor-quarantine
awslocal s3 mb s3://harbor-admitted
echo "Buckets harbor-quarantine and harbor-admitted successfully created."
