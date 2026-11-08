# Cenários para simular

Com tudo no ar (`docker compose up --build`), a lista completa de QR Codes de teste, com o que
deve acontecer em cada um, está em:

```bash
curl -s localhost:8081/simulador/qrcodes | python3 -m json.tool
```

Os scripts de `scripts/` poupam digitação:

```bash
./scripts/ler.sh cob-ativa                         # lê o QR do cenário (conta empresa-a)
./scripts/confirmar.sh <id> '{"respostaPagador": "NF 123"}'
curl -s localhost:8080/api/pagamentos/<id> | python3 -m json.tool
```

## Contas PJ do laboratório

| id | Saldo | Cheque empresarial | Limite Pix Agendado/dia |
|---|---|---|---|
| `empresa-a` | 10.000,00 | 5.000,00 | 20.000,00 |
| `empresa-sem-saldo` | 10,00 | 0,00 | 1.000,00 |

```bash
curl -s localhost:8080/api/simulacao/contas | python3 -m json.tool
curl -s -X PUT localhost:8080/api/simulacao/contas/empresa-a/saldo -H 'Content-Type: application/json' -d '{"saldo": 0}'
curl -s -X PUT localhost:8080/api/simulacao/contas/empresa-a/limite-pix-agendado -H 'Content-Type: application/json' -d '{"limite": 100}'
curl -s -X POST localhost:8080/api/simulacao/contas/reiniciar
```

## Pagar agora (máquina M2)

| Cenário | O que exercita |
|---|---|
| `estatico-com-valor` | valor travado, txid na pacs.008 (QRES) |
| `estatico-sem-valor` | pagador digita o valor: `{"valor": 42.50}` |
| `estatico-pf` | CPF mascarado na tela |
| `cob-ativa` | fluxo feliz do dinâmico: responda `{"respostaPagador": "NF 98765"}` e veja o RmtInf na pacs.008 |
| `cob-valor-livre` | valor editável com sugestão zero |
| `cob-troco` | `{"valorTroco": 20}` gera GSCB com total 100,00 |
| `cob-saque` | OTHR |
| `cob-credito` | aviso de uso do cheque empresarial |
| `estatico-sem-saldo` (conta `empresa-sem-saldo`) | RECUSADO_INTERNO |
| `cob-conta-encerrada` | SPI rejeita com AC03, estorno |
| `cob-spi-lento` | sem resposta, EM_VERIFICACAO, consulta, LIQUIDADO |
| `cob-spi-sumiu` | sem resposta e a ordem não existe: estorno depois de 3 consultas |

## Recusas na leitura

`estatico-fraude`, `estatico-chave-inexistente`, `estatico-dict-fora`, `estatico-crc-errado`,
`estatico-txid-invalido`, `cob-concluida`, `cob-removida`, `cob-expirada`, `cob-410`,
`cob-indisponivel`, `cob-assinatura-adulterada`, `cob-alg-hs256`, `cob-jku-outro-host`, `cob-sem-kid`,
`cob-txid-curto`, `cob-fraude`, `site-homologacao`, `site-revogado`, `site-nao-cadastrado`,
`site-tls-antigo`, `site-wildcard`, `site-sem-ev`, `url-com-protocolo`, `cobv-vencida`,
`cobv-incoerente`, `cobv-paga-no-boleto`.

## Agendamento (máquina M3)

O relógio do pagador pode ser adiantado. O agendamento roda por **janelas** que você dispara:
janela 1 é a primeira tentativa do dia D; janela 2 é a retentativa do mesmo dia.

```bash
# 1. Ler e agendar para daqui a 3 dias (troque a data)
./scripts/ler.sh cobv-a-vencer
./scripts/confirmar.sh <id> '{"dataPagamento": "2026-11-12"}'      # -> AGENDADO + comprovante "Agendamento Pix"

# 2. Tentar cancelar depois da meia-noite do dia D (00:30 em Brasília = 03:30 UTC)
curl -s -X POST localhost:8080/api/simulacao/relogio -H 'Content-Type: application/json' -d '{"agora": "2026-11-12T03:30:00Z"}'
curl -s -X POST localhost:8080/api/pagamentos/<id>/cancelamento        # -> CANCELAMENTO_FORA_DO_PRAZO

# 3. Rodar a janela 1 do dia D
curl -s -X POST localhost:8080/api/simulacao/agendamentos/processar -H 'Content-Type: application/json' \
  -d '{"data": "2026-11-12", "janela": 1}' | python3 -m json.tool

# Voltar o relógio ao normal
curl -s -X DELETE localhost:8080/api/simulacao/relogio
```

Antes da janela, prepare um destes "imprevistos" para ver a regra R34 ou R35 em ação:

| Imprevisto | Como provocar | Resultado esperado |
|---|---|---|
| Falta de saldo | agende com `empresa-sem-saldo` | janela 1 vira AGUARDANDO_RETENTATIVA com aviso do horário limite. Ponha saldo e rode a janela 2 para liquidar, ou rode a janela 2 sem saldo para NAO_EFETIVADO |
| Cobrança híbrida paga no boleto | `curl -X POST localhost:8081/simulador/cobrancas/cobv-a-vencer/pagar-no-boleto` | NAO_EFETIVADO |
| Chave mudou de dono | `curl -X POST localhost:8081/simulador/dict/pagamentos@fornecedor-a.com.br/trocar-titular` | NAO_EFETIVADO |
| Recebedor mudou o valor | `curl -X POST localhost:8081/simulador/cobrancas/cobv-a-vencer/alterar-valor` | NAO_EFETIVADO |
| Limite de Pix Agendado reduzido | PUT do limite para 100 | NAO_EFETIVADO sem retentativa |
| Liquidação de madrugada | relógio em 05:00 UTC (02:00 em Brasília) | notificação marcada para 06:00 |
| Acima do limite já no agendamento | `cobv-acima-do-limite` | LIMITE_PIX_AGENDADO na hora de agendar |

Para desfazer os imprevistos: `POST localhost:8081/simulador/cobrancas/reiniciar` e `POST localhost:8081/simulador/dict/reiniciar`.

## Olhando a infraestrutura na LocalStack

```bash
docker compose exec localstack awslocal dynamodb scan --table-name pix-pagamentos --max-items 1
docker compose exec localstack awslocal s3 ls s3://pix-nao-repudio --recursive     # JWS guardados (não-repúdio)
docker compose exec localstack awslocal sqs list-queues
```
