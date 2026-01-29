package com.example.transformation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class TransformationServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(TransformationServiceApplication.class, args);
  }
}

