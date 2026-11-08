package br.lab.pix.simuladores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tudo o que fica FORA do PSP pagador: DICT, sites dos PSPs recebedores (payload JWS),
 * a interface ARQ do BCB (lista CERTQRC) e o SPI. Você não precisa mexer aqui.
 */
@SpringBootApplication
@EnableScheduling
public class SimuladoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimuladoresApplication.class, args);
    }
}
