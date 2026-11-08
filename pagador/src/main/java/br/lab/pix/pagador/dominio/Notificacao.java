package br.lab.pix.pagador.dominio;

import java.time.LocalDateTime;

/** Notificação ao pagador e o horário (de Brasília) em que deve ser enviada. */
public record Notificacao(String texto, LocalDateTime enviarEm) {
}
