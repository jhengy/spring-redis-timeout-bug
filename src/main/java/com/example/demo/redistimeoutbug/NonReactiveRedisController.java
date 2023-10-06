package com.example.demo.redistimeoutbug;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.Map;

@RestController
public class NonReactiveRedisController {
    private final StringRedisTemplate stringRedisTemplate;

    public NonReactiveRedisController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping(value = "/hset-non-reactive")
    public ResponseEntity<String> hset() {
        stringRedisTemplate
            .opsForHash()
            .putAll("test-key", Map.of("field1", "value1"));
        return ResponseEntity.ok("/hset-non-reactive succeeded");
    }

    @GetMapping(value = "/hget-non-reactive")
    public ResponseEntity<String> hget() {
        Instant start = Instant.now();
        final Map<Object, Object> hash = stringRedisTemplate
            .opsForHash()
            .entries("test-key");

        final LettuceConnectionFactory connectionFactory = (LettuceConnectionFactory) (stringRedisTemplate.getConnectionFactory());
        long commandTimeout = connectionFactory.getClientConfiguration().getCommandTimeout().toSeconds();
        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
        String msg = String.format("redisCommandTimeout=%ss, timeElapsed=%dms, hash=%s", commandTimeout, timeElapsed, hash);

        return ResponseEntity.ok(msg);
    }
}

