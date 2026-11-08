# Fontes oficiais (versões usadas)

Tudo aqui foi conferido nos documentos públicos do Banco Central do Brasil em **outubro/novembro de 2026**.
O Pix muda com frequência: antes de levar uma regra para produção, confira a versão vigente.

| Documento | Versão usada | Onde | O que tem para o pagador |
|---|---|---|---|
| Manual de Padrões para Iniciação do Pix | **2.10.0** (IN BCB 769, de 19/08/2026) | [PDF](https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/II_ManualdePadroesparaIniciacaodoPix.pdf) | BR Code, tipos de QR, URL, payload JSON, txid, valores, Anexo V (mapeamento para a pacs.008) |
| Manual de Padrões para Iniciação do Pix | **2.11.0-rc1** (29/10/2026, versão candidata) | [PDF](https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/versoes_futuras/II_ManualdePadroesparaIniciacaodoPix-versao2-11-rc.pdf) | Status novos da cobrança híbrida, grafia "REMOVIDA_", split tributário (Anexo VII) |
| Requisitos Mínimos para a Experiência do Usuário | **7.3** (dez/2025) | [PDF](https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/IV_RequisitosMinimosparaExperienciadoUsuario.pdf) | Caps. 06 e 07 (pagar por QR), 12 (Pix Agendado), 19 (Internet Banking): o que mostrar, mensagens obrigatórias, comprovantes |
| Requisitos Mínimos para a Experiência do Usuário | **7.4** (IN BCB 774/2026), vigente a partir de **01/03/2027** | link na capa da 7.3 | Comprovante sem propaganda, proibido salvar dados de conta de pagamento por QR, mensagem de marcação de fraude |
| Manual de Segurança do SFN, Volume II | **6.00** (Comunicado 45.351, de 08/06/2026) | [PDF](https://www.bcb.gov.br/content/estabilidadefinanceira/cedsfn/Manual%20de%20Seguran%C3%A7a%20do%20SFN%20-%20Vol.II%20-%20v6_00.pdf) | Seção 4 (segurança do QR dinâmico: TLS, JWS, JWK), seção 5 (certificados, lista CERTQRC, revogação) |
| Informe SPI 007/2026 | 08/06/2026 | [PDF](https://bcb.gov.br/content/estabilidadefinanceira/informesspi/InformeSPI-007-2026.pdf) | Certificados do Pix exclusivos por finalidade (prazo de produção: 06/09/2026) |
| Regulamento Pix (Res. BCB 1/2020) alterado pela Res. BCB 587 | 18/09/2026 | [Voto BCB 105/2026](https://normativos.bcb.gov.br/Votos/BCB/2026105/Voto_do_BC_105_2026.pdf) | Cobrança híbrida (boleto + QR com vencimento) a partir de 01/02/2027, art. 39 (rejeição) e art. 41-A |
| API Pix (OpenAPI) | tag de mesma versão do manual | [github.com/bacen/pix-api](https://github.com/bacen/pix-api) | Formatos exatos dos campos do payload |

O que este laboratório **simplifica** e você deve conferir no material oficial:

- **Códigos de rejeição do SPI**: aqui são só AC03, AC06 e AB03. A lista real está no Catálogo de Mensagens do Pix.
- **pacs.008 e pacs.002**: aqui são JSONs simplificados. As mensagens reais são XML ISO 20022.
- **Calendário**: usa só feriados nacionais. O real considera feriados estaduais e municipais do pagador, que é o motivo de o `codMun` ir na consulta da cobrança com vencimento.
- **TLS e certificado do site**: são informados pelo simulador em cabeçalhos HTTP. No mundo real, saem do handshake HTTPS.
- **Estados do pagamento e regras R29 e R34**: são desenho de engenharia, não texto do BCB. Estão marcados assim no código.
