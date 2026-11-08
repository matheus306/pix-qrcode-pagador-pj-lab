#!/bin/bash
# Roda dentro da LocalStack quando ela fica pronta: cria filas, tabela e bucket do laboratório.
set -euo pipefail

awslocal sqs create-queue --queue-name spi-pacs008 >/dev/null
awslocal sqs create-queue --queue-name spi-pacs002 >/dev/null

awslocal dynamodb create-table \
  --table-name pix-pagamentos \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST >/dev/null

awslocal s3 mb s3://pix-nao-repudio >/dev/null

echo "Recursos do laboratório Pix criados: SQS spi-pacs008/spi-pacs002, DynamoDB pix-pagamentos, S3 pix-nao-repudio"
