# Roteiro das 38 regras do pagador PJ

Cada regra é **um método vazio** em `pagador/src/main/java/br/lab/pix/pagador/regras/`. O Javadoc
explica a norma e de onde ela vem. Cada uma tem testes em `pagador/src/test/java/.../regras/`,
que são o gabarito.

**Como estudar uma regra:**

1. Leia o Javadoc do método e, se quiser, a seção citada do documento do BCB ([FONTES.md](FONTES.md)).
2. Leia os testes da regra: eles dizem exatamente o que se espera, inclusive os casos de borda.
3. Implemente e rode só os testes dela:
   ```bash
   mvn -pl pagador test -Dtest='RegrasLeituraQrTest$R01*'
   ```
4. Rode o fluxo real (`docker compose up --build`) com um cenário que usa a regra ([CENARIOS.md](CENARIOS.md)).
   Enquanto faltar alguma regra no caminho, a API responde **HTTP 501** dizendo qual é a próxima.

A ordem abaixo segue o fluxo: em cada **marco**, um pedaço novo do sistema passa a funcionar.

---

## Marco 1: ler um QR Code estático

Cenário de teste: `./scripts/ler.sh estatico-com-valor`

| # | Regra | Método | Fonte |
|---|---|---|---|
| R01 | CRC16 do BR Code confere | `RegrasLeituraQr.crcValido` | Manual de Iniciação, exemplos 2.6.3/2.7.2; nota 28 |
| R02 | Achar o template do Pix (IDs 26 a 51, GUI `br.gov.bcb.pix`) | `RegrasLeituraQr.idTemplatePix` | Manual de Iniciação, 2.5 |
| R03 | Estático, dinâmico ou composto | `RegrasLeituraQr.classificar` | Manual de Iniciação, 2.4 e 2.8 |
| R04 | Txid do estático (até 25 alfanuméricos, `***` = sem txid) | `RegrasLeituraQr.validarTxidEstatico` | Manual de Iniciação, 2.6.2 |
| R15 | Formato de valor `\d{1,10}\.\d{2}` | `RegrasCobranca.valorNoFormato` | Manual de Iniciação, 2.7.1.1 |
| R21 | Interpretar o DICT: inexistente, fora do ar, **fundada suspeita de fraude** | `RegrasExibicao.interpretarConsultaDict` | UX cap. 06, itens 06 a 08 |
| R22 | Nome e documento do recebedor (nome fantasia, CNPJ sem máscara, CPF mascarado, nunca agência/conta) | `RegrasExibicao.montarDestinatario` | UX caps. 06 e 07, item 01 |
| R25 | Avisar o uso de crédito (cheque empresarial) antes de confirmar | `RegrasExibicao.avisoUsoDeCredito` | UX caps. 06 e 07, item 05 |

## Marco 2: pagar o QR estático até "Pix confirmado"

Cenário de teste: `estatico-com-valor`, `estatico-sem-valor`, `estatico-sem-saldo` (conta `empresa-sem-saldo`)

| # | Regra | Método | Fonte |
|---|---|---|---|
| R24 | Sem "Descrição" livre em QR; só a resposta à `solicitacaoPagador`, até 140 caracteres | `RegrasExibicao.validarRespostaAoPagador` | UX cap. 07; Manual de Iniciação, 2.7.1.1 |
| R16 | O que o pagador pode alterar (modalidadeAlteracao, saque/troco) | `RegrasCobranca.validarValoresInformados` | Manual de Iniciação, 2.7.1.1 ("Em síntese") |
| R17 | Valores da ordem: IPAY, GSCB (troco), OTHR (saque) | `RegrasCobranca.valoresDaOrdem` | Manual de Iniciação, Anexo V 2.2 |
| R26 | Máquina de estados do pagamento | `RegrasPagamento.transicaoPermitida` | [MAQUINA_DE_ESTADOS.md](MAQUINA_DE_ESTADOS.md) |
| R27 | EndToEndId (E + ISPB + data UTC + 11) | `RegrasPagamento.gerarEndToEndId` | Padrão do Pix (Catálogo de Mensagens) |
| R28 | Montar a pacs.008: QRES/QRDN, chave, TxId, RmtInf | `RegrasPagamento.montarPacs008` | Manual de Iniciação, Anexo V 2.1 e 2.2 |
| R30 | Mensagens específicas de erro (saldo, conta inexistente, tempo...) | `RegrasPagamento.mensagemDeRejeicao` | UX cap. 06 item 10, cap. 07 item 12 |
| R36 | Notificação ao concluir (nome do recebedor e valor) | `RegrasAgendamento.notificar` | UX cap. 06 item 12, cap. 12 |
| R37 | Comprovante: campos mínimos e o que é proibido mostrar | `RegrasComprovante.montarComprovante` | UX cap. 06 item 13, cap. 07 item 14 |

## Marco 3: QR dinâmico e a segurança do payload

Cenários de teste: `cob-ativa`, depois todos os `cob-*` e `site-*`

| # | Regra | Método | Fonte |
|---|---|---|---|
| R05 | URL do QR: sem protocolo, até 77 caracteres, caracteres válidos | `RegrasLeituraQr.validarUrlLocation` | Manual de Iniciação, 2.5.2 |
| R06 | `/cob/`, `/cobv/` ou `/rec/` | `RegrasLeituraQr.tipoCobranca` | Manual de Iniciação, 2.5.2 |
| R07 | Site `-h` (homologação) é recusado em produção | `RegrasSegurancaQr.validarAmbienteDoHost` | Segurança SFN Vol. II, 4.1 e 4.3 |
| R11 | HTTP 404/410 = cobrança encerrada; resto = recebedor fora | `RegrasCobranca.interpretarHttpDoPayload` | Manual de Iniciação, 2.7.3; UX cap. 07 item 04 |
| R08 | Certificado do site: lista CERTQRC, TLS 1.2+, EV, sem wildcard, host no CN/SAN, **revogação** | `RegrasSegurancaQr.validarCertificadoDoSite` | Segurança SFN Vol. II, 4.1, 4.3, 5.1, 5.2 |
| R09 | Algoritmo do JWS (proibidos `none` e `HS*`) | `RegrasSegurancaQr.validarAlgoritmo` | Segurança SFN Vol. II, 4.2 |
| R10 | Cabeçalho JWS x JWK Set (kid, x5t, jku no mesmo site, key_ops) e assinatura | `RegrasSegurancaQr.validarCabecalhoJws` | Segurança SFN Vol. II, 4.2 e 4.3 |
| R12 | Só `ATIVA` paga (inclui os status novos da cobrança híbrida) | `RegrasCobranca.validarStatus` | Manual de Iniciação 2.7.1 e 2.11-rc |
| R14 | Txid do dinâmico: 26 a 35 alfanuméricos | `RegrasCobranca.validarTxidDinamico` | Manual de Iniciação, 2.7.1.1 |
| R13 | Expiração da cobrança imediata (padrão de 24h) | `RegrasCobranca.validarExpiracao` | Manual de Iniciação, 2.7.1.1 |
| R23 | Limpar `infoAdicionais` (sem script) | `RegrasExibicao.sanitizarTexto` | UX cap. 07 |

## Marco 4: quando o SPI não responde

Cenários de teste: `cob-spi-lento`, `cob-spi-sumiu`, `cob-conta-encerrada`

| # | Regra | Método | Fonte |
|---|---|---|---|
| R29 | Nunca reenviar sem saber o que houve com a primeira ordem | `RegrasPagamento.decidirAposTimeout` | Regra de engenharia (todo PSP precisa) |

## Marco 5: cobrança com vencimento (o "boleto em Pix")

Cenários de teste: `cobv-a-vencer`, `cobv-em-atraso`, `cobv-vencida`, `cobv-incoerente`, `cobv-paga-no-boleto`

| # | Regra | Método | Fonte |
|---|---|---|---|
| R18 | Parâmetros da consulta: `codMun` (cadastro do pagador) e `DPP` | `RegrasCobranca.parametrosConsultaCobv` | Manual de Iniciação, 2.7.1.2 |
| R20 | `valor.final` coerente (o pagador não recalcula, mas confere) | `RegrasCobranca.validarCoerenciaValorFinal` | Manual de Iniciação, Anexo V 2.2; 2.11-rc 2.7.1 |
| R19 | Até quando dá para pagar (validade, prorrogação para dia útil) | `RegrasCobranca.dataLimiteCobv` | Manual de Iniciação, 2.7.1.2, notas 44 e 45 |

## Marco 6: Pix Agendado

Cenários de teste: `cobv-a-vencer` com `dataPagamento`, `cobv-acima-do-limite`, e as janelas do dia D ([CENARIOS.md](CENARIOS.md))

| # | Regra | Método | Fonte |
|---|---|---|---|
| R31 | O que pode ser agendado por QR, para quando | `RegrasAgendamento.validarPedidoDeAgendamento` | UX cap. 12 |
| R32 | Limite diário de Pix Agendado | `RegrasAgendamento.validarLimitePixAgendado` | UX cap. 12, item 17 |
| R33 | Cancelar até 23:59 do dia anterior (fuso de Brasília!) | `RegrasAgendamento.podeCancelarAgendamento` | UX cap. 12 |
| R34 | Revalidar no dia: cobrança, titular da chave, valor | `RegrasAgendamento.revalidarNoDia` | UX cap. 12 item 21; Res. BCB 587 art. 39 (recomendação do lab) |
| R35 | Retentativa no mesmo dia (falta de saldo) | `RegrasAgendamento.decidirRetentativa` | UX cap. 12 |

(R36 e R37 do Marco 2 também cobrem as notificações e o comprovante de agendamento.)

## Marco 7: o que vem por aí

| # | Regra | Método | Fonte |
|---|---|---|---|
| R38 | Split tributário (IBS/CBS): quando a pacs.008 pode levar o bloco de tributos | `RegrasSplitTributario.podeInformarTributo` | Manual de Iniciação 2.11-rc, Anexo VII (prazo a definir) |

---

## Regras que o laboratório não transforma em código (mas que você precisa saber)

- **Não salvar a chave do QR como contato**: no dinâmico, nunca; no estático, quando o TxId estiver preenchido (UX cap. 06 item 14, cap. 07 item 15). A partir de 01/03/2027 (UX 7.4) também é proibido salvar os dados de conta desses pagamentos.
- **Pix Copia e Cola e Pix por aproximação** seguem exatamente as mesmas regras do QR. Só muda a forma de obter o texto (Manual de Iniciação, 3.1 e 3.4).
- **No Internet Banking PJ**, o UX recomenda um campo "Pix via código" (Copia e Cola) com "Pagar hoje / Agendar" para cobrança com vencimento (UX cap. 19).
- **Lista CERTQRC**: baixar no máximo 1 vez a cada 24h, com cache. Certificados novos devem chegar aos seus clientes em até 7 dias depois de ativados (Segurança SFN Vol. II, 5.2). O `ClienteArq` faz o cache.
- **Não-repúdio**: guardar o JWS, o certificado e a chave pública de cada transação (Segurança SFN Vol. II, 4.3). O `ArquivoNaoRepudio` grava no S3.
- **Comprovante sem propaganda, links ou ofertas**: vale a partir de 01/03/2027 (UX 7.4).
