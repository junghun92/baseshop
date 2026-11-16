package com.baseshop.board.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class BoardViewCountRedisRepository {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final StringRedisTemplate redisTemplate;

    // 누적 조회수
    public Long read(Long boardId) {
        Double result = redisTemplate.opsForZSet().score(viewCountKey(), String.valueOf(boardId));
        return result == null ? 0L : result.longValue();
    }

    // 조회수 +1 (누적 + 오늘), 반환값은 누적 조회수
    public Long increment(Long boardId) {
        String member = String.valueOf(boardId);
        String dailyKey = dailyViewCountKey(LocalDate.now(ZONE));

        Double result = redisTemplate.opsForZSet().incrementScore(viewCountKey(), member, 1); // 누적 조회수
        redisTemplate.opsForZSet().incrementScore(dailyKey, member, 1); // 일별 조회수
        redisTemplate.expire(dailyKey, Duration.ofDays(8));

        return result == null ? 0L : result.longValue();
    }

    // 누적에서 삭제 (일별 키는 8일 뒤 자동 삭제, 인기글 조회 시 삭제된 글은 제외)
    public void deleteByBoardId(Long boardId) {
        redisTemplate.opsForZSet().remove(viewCountKey(), String.valueOf(boardId));
    }

    // 최근 7일 조회수 상위 N개 (게시글 ID, 순위 순서)
    public List<Long> readWeeklyTop(int size) {
        LocalDate today = LocalDate.now(ZONE);

        // 1. 어제 ~ 6일 전 키 목록 만들기
        List<String> otherKeyList = IntStream.rangeClosed(1, 6)
                .mapToObj(day -> dailyViewCountKey(today.minusDays(day)))
                .toList();

        // 2. 오늘 키 + 위 6개 키를 합산해서 weekly 키에 저장
        redisTemplate.opsForZSet().unionAndStore(dailyViewCountKey(today), otherKeyList, weeklyViewCountKey());

        // 3. weekly 키에서 상위 N개 조회 (점수 높은 순)
        Set<String> result = redisTemplate.opsForZSet().reverseRange(weeklyViewCountKey(), 0, size - 1);

        return result == null ? List.of() : result.stream().map(Long::valueOf).toList();
    }

    private String viewCountKey() {
        return "board:viewCount";
    }

    private String dailyViewCountKey(LocalDate date) {
        return "board:viewCount:" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String weeklyViewCountKey() {
        return "board:viewCount:weekly";
    }

}
