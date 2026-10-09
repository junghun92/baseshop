package com.baseshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Optional;

@EnableJpaAuditing
@SpringBootApplication
public class BaseshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseshopApplication.class, args);
    }

    /**
     * BaseEntity 등록자, 수정자 데이터 등록
     */
    @Bean
    public AuditorAware<String> auditorProvider(){
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                .map(Principal::getName)
                .or(() -> Optional.of("system"));
    }

}
