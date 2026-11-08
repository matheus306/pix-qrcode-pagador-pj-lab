package br.lab.pix.pagador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PagadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagadorApplication.class, args);
    }
}
