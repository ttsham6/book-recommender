package com.ttsham6.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ttsham6.api","com.ttsham6.shared"})
public class WebAPiApplication {

  public static void main(String[] args) {
    SpringApplication.run(WebAPiApplication.class, args);
  }
}
