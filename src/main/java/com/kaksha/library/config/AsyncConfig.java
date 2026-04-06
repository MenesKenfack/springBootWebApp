package com.kaksha.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async Configuration for NFR-03 Availability and NFR-04 Performance.
 * Enables asynchronous processing for long-running operations like backups.
 * 
 * Thread pool configuration in application.properties:
 * - Core pool size: 10 threads
 * - Max pool size: 20 threads
 * - Queue capacity: 100 tasks
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Thread pool configuration is handled via application.properties
    // Custom executors can be defined here if needed
}
