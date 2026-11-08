package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.Recusa;
import br.lab.pix.pagador.dominio.TipoCobranca;
import br.lab.pix.pagador.dominio.TipoQr;
import br.lab.pix.pagador.emv.EmvDados;

import java.util.Optional;

/**
 * Bloco 1: ler e entender o BR Code.
 * Fonte principal: Manual de Padrões para Iniciação do Pix (v2.10.0), seções 2.4 a 2.8.
 */
public class RegrasLeituraQr {

    public static final String GUI_PIX = "br.gov.bcb.pix";

    /**
     * R01: o CRC16 do BR Code confere?
     * <p>
     * O campo 63 (tamanho 04) é sempre o último. O CRC é calculado sobre o texto inteiro
     * ATÉ "6304" (inclusive), com polinômio 0x1021 e valor inicial 0xFFFF (CRC-16/CCITT-FALSE),
     * e escrito em 4 dígitos hexadecimais. Compare sem diferenciar maiúsculas de minúsculas.
     * Texto nulo, curto demais ou sem "6304" na posição certa: inválido.
     */
    public boolean crcValido(String brCode) {
        throw new RegraNaoImplementada("R01", "RegrasLeituraQr.crcValido");
    }

    /**
     * R02: em qual ID (26 a 51) está o template do Pix?
     * <p>
     * O BR Code pode ter vários "Merchant Account Information" (IDs 26 a 51), um por arranjo.
     * O do Pix é o que tem o subcampo 00 (GUI) igual a "br.gov.bcb.pix", sem diferenciar
     * maiúsculas de minúsculas (seção 2.5, nota 10). Devolva o primeiro que achar, ou vazio.
     */
    public Optional<String> idTemplatePix(EmvDados emv) {
        throw new RegraNaoImplementada("R02", "RegrasLeituraQr.idTemplatePix");
    }

    /**
     * R03: que tipo de QR é este?
     * <ul>
     *   <li>COMPOSTO: existe um template entre 80 e 99 com GUI do Pix e subcampo 25 (URL de recorrência).</li>
     *   <li>DINAMICO: o template do Pix tem subcampo 25 (URL do payload).</li>
     *   <li>ESTATICO: o template do Pix tem subcampo 01 (chave).</li>
     *   <li>INVALIDO: nenhum dos casos acima.</li>
     * </ul>
     */
    public TipoQr classificar(EmvDados emv, String idTemplatePix) {
        throw new RegraNaoImplementada("R03", "RegrasLeituraQr.classificar");
    }

    /**
     * R04: o txid do QR estático (campo 62, subcampo 05) é válido?
     * <p>
     * Nulo ou "***" = o recebedor não usou txid (válido). Caso contrário: de 1 a 25 caracteres,
     * só letras (a-z, A-Z) e dígitos (seção 2.6.2). Se inválido: QR_INVALIDO.
     */
    public Optional<Recusa> validarTxidEstatico(String txid) {
        throw new RegraNaoImplementada("R04", "RegrasLeituraQr.validarTxidEstatico");
    }

    /**
     * R05: a URL do QR dinâmico (subcampo 25) é aceitável ANTES de qualquer acesso?
     * <p>
     * Seção 2.5.2: sem prefixo de protocolo (nada de "https://"), no máximo 77 caracteres e sem
     * caracteres inválidos. Formato: {fqdn}/{caminho}/{token}. Aceite apenas letras, dígitos,
     * '.', '-', '_', '~' e '/', com pelo menos um '/' depois do domínio. Se inválida: QR_INVALIDO.
     */
    public Optional<Recusa> validarUrlLocation(String url) {
        throw new RegraNaoImplementada("R05", "RegrasLeituraQr.validarUrlLocation");
    }

    /**
     * R06: a URL aponta para cobrança imediata, com vencimento ou recorrência?
     * <p>
     * Olhe o segmento do caminho imediatamente antes do token (o último segmento):
     * "cobv" = COM_VENCIMENTO, "rec" = RECORRENCIA, qualquer outra coisa (inclusive "cob"
     * ou nenhum segmento) = IMEDIATA.
     */
    public TipoCobranca tipoCobranca(String url) {
        throw new RegraNaoImplementada("R06", "RegrasLeituraQr.tipoCobranca");
    }
}
