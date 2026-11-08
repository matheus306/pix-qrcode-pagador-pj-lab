package br.lab.pix.pagador.infra;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Relógio que dá para adiantar, para simular "o dia do agendamento" sem esperar. */
@Component
public class Relogio {

    public static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private volatile Duration deslocamento = Duration.ZERO;

    public Instant agora() {
        return Instant.now().plus(deslocamento);
    }

    public ZonedDateTime agoraBrasilia() {
        return agora().atZone(BRASILIA);
    }

    public LocalDateTime agoraLocalBrasilia() {
        return agoraBrasilia().toLocalDateTime();
    }

    public LocalDate hoje() {
        return agoraBrasilia().toLocalDate();
    }

    public void definir(Instant novoAgora) {
        deslocamento = Duration.between(Instant.now(), novoAgora);
    }

    public void voltarAoNormal() {
        deslocamento = Duration.ZERO;
    }
}
