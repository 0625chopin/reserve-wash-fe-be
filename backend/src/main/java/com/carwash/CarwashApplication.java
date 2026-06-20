package com.carwash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 2차 백엔드 진입점 (Spring Boot)
@SpringBootApplication
public class CarwashApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarwashApplication.class, args);
    }
}
