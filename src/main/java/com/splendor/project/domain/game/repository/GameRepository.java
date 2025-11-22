package com.splendor.project.domain.game.repository;

import com.splendor.project.domain.game.entity.GameSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class GameRepository {

    private final RedisTemplate<String, GameSession> redisTemplate;

    // Redis Key 접두사
    private static final String KEY_PREFIX = "game:";
    // 데이터 만료 시간
    private static final long TIMEOUT_HOURS = 3;

    public void save(GameSession gameSession) {
        String key = KEY_PREFIX + gameSession.getRoomId();
        redisTemplate.opsForValue().set(key, gameSession, TIMEOUT_HOURS, TimeUnit.HOURS);
    }

    public Optional<GameSession> findById(Long roomId) {
        String key = KEY_PREFIX + roomId;
        GameSession gameSession = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(gameSession);
    }

    public void deleteById(Long roomId) {
        String key = KEY_PREFIX + roomId;
        redisTemplate.delete(key);
    }
}
