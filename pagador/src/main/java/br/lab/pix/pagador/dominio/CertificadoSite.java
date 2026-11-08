package br.lab.pix.pagador.dominio;

import java.util.List;

/**
 * Certificado do site de QR Codes do PSP recebedor, como aparece na lista CERTQRC do BCB
 * (Manual de Segurança do SFN, Vol. II, seção 5.2), já com o resultado da verificação de revogação.
 *
 * @param nomes CN e SANs do certificado
 * @param ev    certificado do tipo EV (Extended Validation)
 */
public record CertificadoSite(List<String> nomes, boolean ev, boolean revogado) {
}
