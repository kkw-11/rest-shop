package com.shop.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * shop-core 모듈 테스트용 설정
 * - @SpringBootApplication이 있는 클래스가 없으면 테스트 실행 불가
 */
@SpringBootApplication(scanBasePackages = {
    "com.shop.core",
    "com.shop.domain",
    "com.shop.common"
})
@EntityScan(basePackages = "com.shop.domain")
@EnableJpaRepositories(basePackages = "com.shop.domain")
public class CoreTestConfiguration {
}
