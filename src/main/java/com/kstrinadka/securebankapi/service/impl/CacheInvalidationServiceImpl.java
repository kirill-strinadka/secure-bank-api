package com.kstrinadka.securebankapi.service.impl;

import com.kstrinadka.securebankapi.config.CacheNames;
import com.kstrinadka.securebankapi.service.CacheInvalidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheInvalidationServiceImpl implements CacheInvalidationService {

    private final CacheManager cacheManager;

    @Override
    public void evictEmailCaches(Long userId) {
        evict(CacheNames.CURRENT_USER_EMAILS, userId);
        clear(CacheNames.USER_SEARCH);
        clear(CacheNames.EMAIL_EXISTS);
    }

    @Override
    public void evictPhoneCaches(Long userId) {
        evict(CacheNames.CURRENT_USER_PHONES, userId);
        clear(CacheNames.USER_SEARCH);
        clear(CacheNames.PHONE_EXISTS);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
