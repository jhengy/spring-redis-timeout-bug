package com.example.demo.redistimeoutbug;

import io.lettuce.core.*;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.*;

import java.time.Duration;

@Configuration
public class RedisConfig {
    Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    // setting ClientOptions seems to interfere with spring.redis.timeout
    @Bean
    @ConditionalOnProperty("customize-client-options")
    public LettuceClientConfigurationBuilderCustomizer tcpKeepAliveReplicaBuilderCustomizer() {
        logger.info("Customize Lettuce configuration Client Options");

        SocketOptions socketOptions = SocketOptions.builder()
            .keepAlive(SocketOptions.KeepAliveOptions.builder()
                .enable(true)
                .idle(Duration.ofSeconds(60))
                .count(3)
                .interval(Duration.ofSeconds(60))
                .build()
            ).build();

        return builder -> builder.clientOptions(
            ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(TimeoutOptions.enabled())
                .build()
        );
    }
}
