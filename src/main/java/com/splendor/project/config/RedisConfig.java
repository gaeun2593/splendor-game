package com.splendor.project.config;

import com.splendor.project.domain.game.entity.GameSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * GameSession 객체를 JSON으로 직렬화하여 Redis에 저장하는 Template 설정
     */
    @Bean
    public RedisTemplate<String, GameSession> gameSessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, GameSession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 저장 (예: "game:1")
        template.setKeySerializer(new StringRedisSerializer());

        // Value는 JSON으로 저장 (GameSession 객체 통째로)
        // GenericJackson2JsonRedisSerializer를 사용하면 객체 타입 정보를 포함하여 저장/복원
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
