#!/usr/bin/env bash
# Lê um QR Code de teste pelo nome do cenário (ver: curl localhost:8081/simulador/qrcodes).
#   ./scripts/ler.sh cob-ativa            -> conta empresa-a
#   ./scripts/ler.sh estatico-sem-saldo empresa-sem-saldo
set -euo pipefail
CENARIO="${1:?informe o cenário, ex.: cob-ativa}"
CONTA="${2:-empresa-a}"
BRCODE=$(curl -s localhost:8081/simulador/qrcodes | python3 -c "
import json, sys
for q in json.load(sys.stdin):
    if q['cenario'] == sys.argv[1]:
        print(q['brCode']); break
else:
    sys.exit('cenário não encontrado: ' + sys.argv[1])
" "$CENARIO")
python3 -c "import json,sys; print(json.dumps({'idConta': sys.argv[1], 'brCode': sys.argv[2]}))" "$CONTA" "$BRCODE" \
  | curl -s -X POST localhost:8080/api/leituras -H 'Content-Type: application/json' -d @- | python3 -m json.tool --no-ensure-ascii
