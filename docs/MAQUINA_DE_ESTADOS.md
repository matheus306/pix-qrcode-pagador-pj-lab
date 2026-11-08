# Máquinas de estados do pagador

> O BCB **não define** estados internos do PSP pagador. Ele define regras: o que mostrar, quando
> recusar, quando notificar, e os status da cobrança. As máquinas abaixo são um **desenho** para
> cumprir essas regras. A tabela de transições é a regra **R26** (`RegrasPagamento.transicaoPermitida`).

## M1: leitura do QR (`FluxoLeitura`, `LeitorDePayload`)

A leitura não cria estado até dar certo. Qualquer recusa no caminho volta como HTTP 422, com o código e a mensagem que o pagador vê.

```mermaid
flowchart TD
    A[BR Code lido / colado] --> B{R01 CRC confere?}
    B -- não --> X[QR_INVALIDO]
    B -- sim --> C{R02 template Pix?}
    C -- não --> X
    C -- sim --> D{R03 tipo}
    D -- estático --> E[R04 txid, R15 valor]
    D -- composto --> Y[Fora do escopo do lab]
    D -- dinâmico --> F[R05 URL, R06 tipo de cobrança]
    F --> G[R07 site -h? ]
    G --> H[GET payload, R18 codMun/DPP se cobv]
    H --> I[R11 HTTP, R08 certificado, R09 alg, R10 JWS]
    I --> J[R12 status, R14 txid, R13 expiração ou R19/R20 vencimento]
    E --> K[DICT: R21]
    J --> K
    K -- fraude / inexistente / fora --> Z[Recusa]
    K -- ok --> L[R22 destinatário, R23 textos, R25 aviso de crédito]
    L --> M((PRONTO_PARA_CONFIRMACAO))
```

## M2 e M3: do "Pagar" ao fim (tabela da regra R26)

```mermaid
stateDiagram-v2
    [*] --> PRONTO_PARA_CONFIRMACAO
    PRONTO_PARA_CONFIRMACAO --> AUTORIZADO: confirma, saldo ok
    PRONTO_PARA_CONFIRMACAO --> RECUSADO_INTERNO: saldo + crédito não cobrem
    PRONTO_PARA_CONFIRMACAO --> CANCELADO_PELO_USUARIO
    PRONTO_PARA_CONFIRMACAO --> AGENDADO: data futura (só cobv)

    AUTORIZADO --> ENVIADO_SPI: débito + pacs.008
    ENVIADO_SPI --> LIQUIDADO: pacs.002 ACSC
    ENVIADO_SPI --> REJEITADO: pacs.002 RJCT (estorna)
    ENVIADO_SPI --> EM_VERIFICACAO: sem resposta (timeout)
    EM_VERIFICACAO --> LIQUIDADO: consulta = liquidado
    EM_VERIFICACAO --> REJEITADO: consulta = rejeitado / não encontrado (estorna)

    AGENDADO --> CANCELADO_PELO_USUARIO: até 23:59 de D-1
    AGENDADO --> REVALIDANDO: dia D, janela 1
    REVALIDANDO --> AUTORIZADO: tudo certo
    REVALIDANDO --> AGUARDANDO_RETENTATIVA: sem saldo / falha operacional
    REVALIDANDO --> NAO_EFETIVADO: cobrança paga/removida, chave mudou, valor mudou, limite
    AGUARDANDO_RETENTATIVA --> REVALIDANDO: dia D, janela 2
    AGUARDANDO_RETENTATIVA --> NAO_EFETIVADO

    LIQUIDADO --> [*]
    REJEITADO --> [*]
    CANCELADO_PELO_USUARIO --> [*]
    RECUSADO_INTERNO --> [*]
    NAO_EFETIVADO --> [*]
```

Por que existe cada estado "do meio":

- **EM_VERIFICACAO**: impede pagar duas vezes. Uma ordem sem resposta pode ter liquidado. A regra R29 manda **consultar**, nunca reenviar.
- **REVALIDANDO**: o agendamento foi feito dias antes. No dia D, a cobrança pode ter sido paga no boleto (cobrança híbrida, a partir de 01/02/2027), a chave pode ter mudado de dono e o recebedor pode ter mudado o valor. É a regra R34.
- **AGUARDANDO_RETENTATIVA**: o UX exige uma nova tentativa no mesmo dia quando falta saldo, com aviso ao cliente do horário limite. Regras R35 e R36.

## Status da cobrança (definidos pelo BCB, só `ATIVA` paga: regra R12)

```mermaid
stateDiagram-v2
    [*] --> ATIVA
    ATIVA --> CONCLUIDA: paga via Pix
    ATIVA --> REMOVIDA_PELO_USUARIO_RECEBEDOR
    ATIVA --> REMOVIDA_PELO_PSP
    ATIVA --> CONCLUIDA_POR_OUTRO_ARRANJO: cobv paga no boleto (2027)
    ATIVA --> REMOVIDA_PELO_PSP_BOLETO: cobv híbrida removida (2027)
```

A v2.10 do manual escreve `REMOVIDO_...` (masculino); a 2.11-rc, `REMOVIDA_...`. O pagador não precisa distinguir: tudo que não é `ATIVA` não paga. A URL também pode responder 404 ou 410 para cobrança encerrada (regra R11).
