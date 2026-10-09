package com.baseshop.security.filter;

import com.baseshop.member.type.MemberRoleType;
import com.baseshop.security.util.JWTUtil;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    /**
     * JWT 필터 예외처리 URL
     */
    private static final Set<String> EXCLUDE_URLS = Set.of(
            "/api/v1/login",
            "/api/v1/reissue",
            "/api/v1/logout"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDE_URLS.contains(request.getRequestURI());
    }

    /**
     * HTTP 요청시 유저 식별
     * 토큰 검증
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveToken(request);

        if(accessToken == null){
            filterChain.doFilter(request, response);
            return;
        }

        try {
            //토큰 -> 사용자 정보 가져오기
            CustomUserDetails userDetails = getCustomUserDetails(response, accessToken);

            if (userDetails == null) {
                return;
            }

            //시큐리티 저장소에 저장
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException e) {
            handleExceptionResponse(response, "토큰이 만료되었습니다."); //재발급 필요
            return;
        } catch (Exception e) {
            handleExceptionResponse(response, "유효하지 않은 토큰입니다.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * AccessToken -> 사용자 정보 가져오기
     */
    private CustomUserDetails getCustomUserDetails(HttpServletResponse response, String accessToken) throws IOException {
        Claims claims = jwtUtil.getClaims(accessToken);

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String tokenType = claims.get("tokenType", String.class);

        Long memberId = claims.get("memberId", Long.class);

        if (!jwtUtil.getAccessTokenName().equals(tokenType)) {
            handleExceptionResponse(response, "유효하지 않은 Access 토큰 입니다.");
            return null;
        }

        return CustomUserDetails.builder()
                .memberId(memberId)
                .username(username)
                .password("")
                .memberRoleType(MemberRoleType.valueOf(role))
                .build();
    }

    /**
     * JWT 예외처리
     */
    private void handleExceptionResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format("{\"error\":\"%s\"}", message);

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * Authorization AccessToken 확인
     */
    private String resolveToken(HttpServletRequest request){
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring(7);
    }

}
