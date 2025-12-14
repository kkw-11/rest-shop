package com.shop.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.shop.admin",
        "com.shop.core",
        "com.shop.domain",
        "com.shop.common"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.shop.domain")
@EntityScan(basePackages = "com.shop.domain")
public class ShopAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopAdminApplication.class, args);
    }
}
