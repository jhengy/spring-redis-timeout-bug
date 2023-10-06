package com.example.demo.redistimeoutbug;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.Map;

@RestController
public class ReactiveRedisController {
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public ReactiveRedisController(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    @GetMapping(value = "/hset-reactive")
    public ResponseEntity<String> hset() {
        reactiveStringRedisTemplate
            .opsForHash()
            .putAll("test-key", Map.of("field1", "value1"))
            .block();
        return ResponseEntity.ok("/hset-reactive succeeded");
    }

    @GetMapping(value = "/hget-reactive")
    public ResponseEntity<String> hget() {
        Instant start = Instant.now();
        final Map<Object, Object> hash = reactiveStringRedisTemplate
            .opsForHash()
            .entries("test-key")
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .block();

        final LettuceConnectionFactory connectionFactory = (LettuceConnectionFactory) (reactiveStringRedisTemplate.getConnectionFactory());
        long commandTimeout = connectionFactory.getClientConfiguration().getCommandTimeout().toSeconds();
        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
        String msg = String.format("redisCommandTimeout=%ss, timeElapsed=%dms, hash=%s", commandTimeout, timeElapsed, hash);

        return ResponseEntity.ok(msg);
    }
}

