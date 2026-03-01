package ru.korevg.fimas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ru.korevg.fimas"})
public class FimasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FimasApplication.class, args);
    }
}
