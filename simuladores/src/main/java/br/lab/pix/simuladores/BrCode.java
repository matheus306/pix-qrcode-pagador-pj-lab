package br.lab.pix.simuladores;

import java.nio.charset.StandardCharsets;

/** Monta BR Codes de exemplo. (Spoiler da regra R01 aqui dentro: tente não olhar o crc16.) */
final class BrCode {

    private BrCode() {
    }

    static String tlv(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    static String estatico(String chave, String valor, String txid, String nome) {
        String mai = tlv("00", "br.gov.bcb.pix") + tlv("01", chave);
        return fechar("000201" + tlv("26", mai) + "52040000" + "5303986"
                + (valor == null ? "" : tlv("54", valor))
                + "5802BR" + tlv("59", nome) + tlv("60", "SAO PAULO")
                + tlv("62", tlv("05", txid == null ? "***" : txid)));
    }

    static String dinamico(String url, String nome) {
        String mai = tlv("00", "br.gov.bcb.pix") + tlv("25", url);
        return fechar("000201" + "010212" + tlv("26", mai) + "52040000" + "5303986"
                + "5802BR" + tlv("59", nome) + tlv("60", "SAO PAULO") + tlv("62", tlv("05", "***")));
    }

    static String fechar(String semCrc) {
        String base = semCrc + "6304";
        return base + crc16(base);
    }

    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
