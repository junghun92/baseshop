package com.baseshop.portone;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PortOneConfig {

    private final String baseUrl;
    private final String secretKey;

    public PortOneConfig(
            @Value("${portone.api.base-url}") String baseUrl,
            @Value("${portone.api.secret}") String secretKey) {

        this.baseUrl = baseUrl;
        this.secretKey = secretKey;
    }

    @Bean
    public RestClient portOneRestClient(){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));   // 연결 타임아웃
        requestFactory.setReadTimeout(Duration.ofSeconds(60));     // 읽기 타임아웃 (포트원 권장 60초 이상)

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "PortOne " + secretKey)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(requestFactory)
                .build();
    }
}
