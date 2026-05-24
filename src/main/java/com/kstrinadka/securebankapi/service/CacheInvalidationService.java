package com.kstrinadka.securebankapi.service;

public interface CacheInvalidationService {

    void evictEmailCaches(Long userId);

    void evictPhoneCaches(Long userId);
}
