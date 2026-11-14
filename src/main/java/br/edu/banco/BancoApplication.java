package br.edu.banco;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BancoApplication {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("dados"));
        SpringApplication.run(BancoApplication.class, args);
    }
}
