package com.ttsham6.batch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ttsham6.batch","com.ttsham6.shared"})
public class BatchApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder(BatchApplication.class).web(WebApplicationType.NONE).run(args);
  }
}
