package com.kaksha.library.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration for NFR-04 Performance Compliance.
 * Enables Spring Cache abstraction with Caffeine as the cache provider.
 * 
 * Cache specifications configured in application.properties:
 * - maximumSize=500 entries per cache
 * - expireAfterWrite=300s (5 minutes)
 * - recordStats for monitoring
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Cache configuration is handled via application.properties
    // Additional cache managers can be defined here if needed
}
