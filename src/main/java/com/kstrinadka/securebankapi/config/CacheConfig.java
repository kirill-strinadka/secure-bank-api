package com.kstrinadka.securebankapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final long MAXIMUM_CACHE_SIZE = 10_000;
    private static final Duration EXPIRE_AFTER_WRITE = Duration.ofMinutes(5);

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(CacheNames.ALL));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_CACHE_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE));
        return cacheManager;
    }
}
