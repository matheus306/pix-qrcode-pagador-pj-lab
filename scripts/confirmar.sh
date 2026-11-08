#!/usr/bin/env bash
# Confirma (paga ou agenda) um pagamento lido.
#   ./scripts/confirmar.sh <id>                                   -> paga agora
#   ./scripts/confirmar.sh <id> '{"valor": 42.50}'                -> paga agora, valor digitado
#   ./scripts/confirmar.sh <id> '{"respostaPagador": "NF 123"}'   -> responde à solicitação do recebedor
#   ./scripts/confirmar.sh <id> '{"dataPagamento": "2026-11-13"}' -> agenda
set -euo pipefail
ID="${1:?informe o id do pagamento}"
CORPO="${2:-{\}}"
curl -s -X POST "localhost:8080/api/pagamentos/$ID/confirmacao" -H 'Content-Type: application/json' -d "$CORPO" \
  | python3 -m json.tool --no-ensure-ascii
