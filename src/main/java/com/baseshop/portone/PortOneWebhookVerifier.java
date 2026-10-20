package com.baseshop.portone;

import com.baseshop.portone.dto.PortOneWebhookReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 포트원 웹훅 서명 검증
 *
 * 포트원 V2 웹훅은 Standard Webhooks 스펙을 따름
 * - 헤더: webhook-id, webhook-timestamp, webhook-signature
 * - 서명: HMAC-SHA256("webhook-id.webhook-timestamp.body") → Base64
 * - 서명 헤더 형식: "v1,서명값" (여러 개면 공백으로 구분)
 *
 * 참고
 * - 포트원 웹훅 연동 가이드 (웹훅 메시지 검증하기, 웹훅 요청 처리 코드 예시)
 *   https://developers.portone.io/opi/ko/integration/webhook/readme-v2?v=v2
 * - 포트원 JVM SDK WebhookVerifier (검증 순서, 시크릿 처리, 서명 비교 방식)
 *   https://github.com/portone-io/server-sdk/blob/main/jvm/lib/src/main/kotlin/io/portone/sdk/server/webhook/WebhookVerifier.kt
 * - Standard Webhooks 스펙 (헤더 이름, v1 형식, 시크릿 형식)
 *   https://github.com/standard-webhooks/standard-webhooks/blob/main/spec/standard-webhooks.md
 */
@Component
@Slf4j
public class PortOneWebhookVerifier {

    private static final String SECRET_PREFIX = "whsec_";
    private static final String SIGNATURE_VERSION = "v1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TOLERANCE_SECONDS = 5 * 60; // 5분

    private final SecretKeySpec secretKey;
    private final ObjectMapper objectMapper;

    public PortOneWebhookVerifier(
            @Value("${portone.webhook-secret}") String webhookSecret,
            ObjectMapper objectMapper) {
        this.secretKey = createSecretKey(webhookSecret);
        this.objectMapper = objectMapper;
    }

    /**
     * 웹훅 검증 + 파싱
     * 순서: 헤더 확인 -> 타임스탬프 검증 -> 서명 비교 -> body 파싱 (포트원 SDK verify() 와 동일)
     * body 는 JSON 파싱 전 원본 문자열이어야 함 (포트원 가이드 코드 예시: "웹훅 검증 시 텍스트로 된 body가 필요합니다")
     */
    public PortOneWebhookReqDto verify(String body, String webhookId, String webhookTimestamp, String webhookSignature) {
        if (webhookId == null || webhookTimestamp == null || webhookSignature == null) {
            throw new IllegalArgumentException("웹훅 헤더 누락");
        }

        // 타임스탬프 검증
        if (!isValidTimestamp(webhookTimestamp)) {
            throw new IllegalArgumentException("웹훅 시간 초과");
        }

        // 서명비교
        byte[] expectedSignature = generateSignature(webhookId, webhookTimestamp, body);
        if (!verifySignature(expectedSignature, webhookSignature)) {
            throw new IllegalArgumentException("유효하지 않은 웹훅 서명");
        }

        return parseBody(body);
    }

    /**
     * 요청 본문 파싱 (서명 검증 통과 후에만 호출)
     */
    private PortOneWebhookReqDto parseBody(String body) {
        return objectMapper.readValue(body, PortOneWebhookReqDto.class);
    }

    /**
     * 시크릿 키 생성
     * "whsec_" 접두사를 떼고 Base64 디코딩한 값이 실제 키
     * 참고: 포트원 SDK WebhookVerifier(secretKey: String) 생성자, Standard Webhooks "Signature scheme"
     */
    private SecretKeySpec createSecretKey(String webhookSecret) {
        String secret = webhookSecret.startsWith(SECRET_PREFIX)
                ? webhookSecret.substring(SECRET_PREFIX.length())
                : webhookSecret;
        return new SecretKeySpec(Base64.getDecoder().decode(secret), HMAC_SHA256);
    }

    /**
     * 타임스탬프 검증
     * 요청이 5분 이상 경과한 경우 무효화
     * 참고: 포트원 SDK verifyTimestamp(), Standard Webhooks "Verifying webhooks"
     */
    private boolean isValidTimestamp(String webhookTimestamp) {
        try {
            long timestamp = Long.parseLong(webhookTimestamp);
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - timestamp) <= TOLERANCE_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 서명 생성
     * "webhook-id, webhook-timestamp, body" 를 시크릿 키로 HMAC-SHA256
     * 참고: 포트원 SDK sign(), Standard Webhooks "Signature scheme"
     */
    private byte[] generateSignature(String webhookId, String webhookTimestamp, String body) {
        try {
            String dataToSign = String.join(".", webhookId, webhookTimestamp, body);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKey);
            return mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("웹훅 서명 생성 실패", e);
        }
    }

    /**
     * 서명 비교
     * 헤더 형식: "v1,서명값" (시크릿 교체 중에는 "v1,서명1 v1,서명2" 처럼 여러 개)
     * 하나라도 일치하면 통과
     * MessageDigest.isEqual: 비교 시간이 일정해서 타이밍 공격 방지
     * 참고: 포트원 SDK verify() 서명 비교 부분, Standard Webhooks "Signature scheme"
     *       포트원 가이드 "무중단으로 시크릿 교체하기" (서명이 여러 개 오는 경우)
     */
    private boolean verifySignature(byte[] expectedSignature, String webhookSignature) {
        for (String versionedSignature : webhookSignature.split(" ")) {
            String[] parts = versionedSignature.split(",", 2); // "v1,서명값" → ["v1", "서명값"]
            if (parts.length != 2 || !SIGNATURE_VERSION.equals(parts[0])) {
                continue;
            }
            if (MessageDigest.isEqual(expectedSignature, decodeSignature(parts[1]))) {
                return true;
            }
        }
        log.warn("웹훅 서명 불일치");
        return false;
    }

    /**
     * 헤더의 서명값(Base64)을 바이트로 변환
     * 형식이 잘못되면 빈 배열 -> 비교에서 불일치 처리 (포트원 SDK 와 동일하게 예외 대신 불일치로 처리)
     */
    private byte[] decodeSignature(String signature) {
        try {
            return Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
