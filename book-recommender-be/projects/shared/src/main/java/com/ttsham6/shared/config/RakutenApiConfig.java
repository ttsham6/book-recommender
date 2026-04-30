package com.ttsham6.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(RakutenApiProperty.class)
public class RakutenApiConfig {

  private final RakutenApiProperty rakutenApiProperty;

  public RakutenApiConfig(RakutenApiProperty rakutenApiProperty) {
    this.rakutenApiProperty = rakutenApiProperty;
  }

  @Bean
  public RestTemplate rakutenRestTemplate() {
    return new RestTemplateBuilder().rootUri(rakutenApiProperty.getUrl()).build();
  }
}
