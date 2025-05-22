package com.small.backend.authservice.config;

import org.modelmapper.ModelMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeneralConfig {
    @Bean
    @LoadBalanced // marks the bean to be intercepted by a client-side load balancer (in this case, Eureka).
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
