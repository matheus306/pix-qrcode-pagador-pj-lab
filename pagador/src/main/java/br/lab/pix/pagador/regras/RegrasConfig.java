package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CalendarioDiasUteis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** As classes de regras são POJOs (fáceis de testar). Aqui elas viram beans para os fluxos. */
@Configuration
public class RegrasConfig {

    @Bean RegrasLeituraQr regrasLeituraQr() { return new RegrasLeituraQr(); }
    @Bean RegrasSegurancaQr regrasSegurancaQr() { return new RegrasSegurancaQr(); }
    @Bean RegrasCobranca regrasCobranca() { return new RegrasCobranca(); }
    @Bean RegrasExibicao regrasExibicao() { return new RegrasExibicao(); }
    @Bean RegrasPagamento regrasPagamento() { return new RegrasPagamento(); }
    @Bean RegrasAgendamento regrasAgendamento() { return new RegrasAgendamento(); }
    @Bean RegrasComprovante regrasComprovante() { return new RegrasComprovante(); }
    @Bean CalendarioDiasUteis calendarioDiasUteis() { return new CalendarioDiasUteis(); }
}
