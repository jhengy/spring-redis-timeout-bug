package com.example.demo.redistimeoutbug;

import io.lettuce.core.*;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class RedisConfig {
    Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    // setting ClientOptions seems to interfere with spring.redis.timeout
    @Bean
    @ConditionalOnProperty("customize-client-options")
    public LettuceClientConfigurationBuilderCustomizer tcpKeepAliveReplicaBuilderCustomizer() {
        logger.info("Customize Lettuce configuration Client Options");

        return builder -> builder.clientOptions(
            ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.enabled())
                .build()
        );
    }
}
