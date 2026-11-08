package br.lab.pix.pagador.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Calendário SIMPLIFICADO: fins de semana + feriados nacionais de 2026 e 2027.
 * No mundo real, a prorrogação considera também feriados estaduais e municipais do
 * pagador (por isso o codMun vai na consulta da cobrança com vencimento).
 */
public class CalendarioDiasUteis {

    private static final Set<LocalDate> FERIADOS = Set.of(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17),
            LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 21), LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 6, 4), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 12, 25),
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 9),
            LocalDate.of(2027, 3, 26), LocalDate.of(2027, 4, 21), LocalDate.of(2027, 5, 1),
            LocalDate.of(2027, 5, 27), LocalDate.of(2027, 9, 7), LocalDate.of(2027, 10, 12),
            LocalDate.of(2027, 11, 2), LocalDate.of(2027, 11, 15), LocalDate.of(2027, 11, 20),
            LocalDate.of(2027, 12, 25));

    public boolean ehDiaUtil(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY && !FERIADOS.contains(data);
    }
}
