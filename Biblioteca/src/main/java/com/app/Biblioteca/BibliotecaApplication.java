package com.app.Biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.app.Biblioteca", "presentationLayer", "bussinesLayer", "persistanceLayer"})
@EnableJpaRepositories(basePackages = "persistanceLayer.repository")
@EntityScan(basePackages = "persistanceLayer.entity")
@EnableScheduling
public class BibliotecaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BibliotecaApplication.class, args);
    }
}