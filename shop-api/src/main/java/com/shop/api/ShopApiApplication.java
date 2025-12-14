package com.shop.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.shop.api",      // API 컨트롤러
        "com.shop.core",     // 서비스 레이어
        "com.shop.domain",   // 엔티티, 리포지토리
        "com.shop.common"    // 공통 (ExceptionHandler 등)
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.shop.domain")
@EntityScan(basePackages = "com.shop.domain")
public class ShopApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApiApplication.class, args);
    }
}