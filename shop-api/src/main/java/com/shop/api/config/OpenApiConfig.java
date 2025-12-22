package com.shop.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shop Commerce API Documentation") // API 이름
                        .version("v1.0.0") // API 버전
                        .description("MSA 전환을 위한 커머스 REST API 명세서")
                );
    }
}
