package com.baseshop.security.config;

import com.baseshop.security.filter.JWTFilter;
import com.baseshop.security.filter.LoginFilter;
import com.baseshop.security.handler.LoginSuccessHandler;
import com.baseshop.security.util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginSuccessHandler loginSuccessHandler;
    private final JWTUtil jwtUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){
        http.csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.formLogin(AbstractHttpConfigurer::disable);

        http.httpBasic(AbstractHttpConfigurer::disable);

        // CORS 설정
        //http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.cors(AbstractHttpConfigurer::disable);

        // 경로별 인가
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/login").permitAll() // 로그인
                        .requestMatchers(HttpMethod.POST, "/api/v1/logout").permitAll() // 로그아웃
                        .requestMatchers(HttpMethod.POST, "/api/v1/reissue").permitAll() // accessToken 재발급

                        .requestMatchers(HttpMethod.POST, "/api/v1/board").hasAnyAuthority("USER", "ADMIN") // 게시글 등록
                        .requestMatchers(HttpMethod.PUT, "/api/v1/board/*").hasAnyAuthority("USER", "ADMIN") // 게시글 수정
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/board/*").hasAnyAuthority("USER", "ADMIN") // 게시글 삭제
                        .requestMatchers(HttpMethod.GET, "/api/v1/board/*").permitAll() // 게시글 조회
                        .requestMatchers(HttpMethod.GET, "/api/v1/board").permitAll() // 전체 게시글 조회

                        .requestMatchers(HttpMethod.GET, "/api/v1/product").permitAll() // 상품 리스트
                        .requestMatchers(HttpMethod.GET, "/api/v1/product/*").permitAll() // 상품상세
                        .requestMatchers(HttpMethod.POST, "/api/v1/product").hasAuthority("ADMIN") // 상품등록
                        .requestMatchers(HttpMethod.PUT, "/api/v1/product/*").hasAuthority("ADMIN") // 상품수정
                        .requestMatchers(HttpMethod.PUT, "/api/v1/product/*/quantity").hasAuthority("ADMIN") // 상품 재고수정
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/product/*").hasAuthority("ADMIN") // 상품 판매중지

                        .requestMatchers(HttpMethod.GET, "/api/v1/cart").hasAnyAuthority("USER", "ADMIN") // 내 장바구니 리스트
                        .requestMatchers(HttpMethod.POST, "/api/v1/cart").hasAnyAuthority("USER", "ADMIN") // 장바구니 등록
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cart/*").hasAnyAuthority("USER", "ADMIN") // 장바구니 상품 수량변경
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cart/*").hasAnyAuthority("USER", "ADMIN") // 장바구니 상품 삭제
                
                        .requestMatchers(HttpMethod.POST, "/api/v1/order").hasAnyAuthority("USER", "ADMIN") // 주문등록
                        .requestMatchers(HttpMethod.GET, "/api/v1/order").hasAnyAuthority("USER", "ADMIN") // 내 주문 리스트
                        .requestMatchers(HttpMethod.PUT, "/api/v1/order/*/cancel").hasAnyAuthority("USER", "ADMIN") // 주문 취소(상태값 변경)

                        .requestMatchers(HttpMethod.POST, "/api/v1/member").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/product/*/file").hasAnyAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll() // 이미지

                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()

                //.anyRequest().hasRole("ADMIN")
        );

        // 예외처리 (인증·인가 실패 응답)
        http.exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED))   // 401: 로그인 안 함
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN))      // 403: 로그인은 했지만 권한 없음
        );

        // 로그인 커스텀 필터 추가
        http.addFilterBefore(
                new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);

        // JWT 필터 추가
        http.addFilterAfter(new JWTFilter(jwtUtil), SecurityContextHolderFilter.class);


        return http.build();
    }

    /**
     * CORS 설정
     */
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        //TODO: 개발환경에 따라 수정
//        config.addAllowedOrigin("http://localhost:5173");
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//        config.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        // 모든 API에 동일한 설정 적용
//        source.registerCorsConfiguration("/**", config);
//
//        return source;
//    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }
}
