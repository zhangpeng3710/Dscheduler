package com.roc.dscheduler.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, List<? extends Trigger>> triggerCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000) // Maximum number of entries in the cache
                .expireAfterWrite(30, TimeUnit.MINUTES) // Cache entries expire after 5 minutes
                .recordStats() // Enable statistics
                .build();
    }

    @Bean
    public Cache<String, Boolean> jobKeysCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000) // Only need one entry for the job keys
                .expireAfterWrite(30, TimeUnit.MINUTES) // Refresh job keys every minute
                .build();
    }
    
    @Bean
    public Cache<String, JobDetail> jobDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000) // Maximum number of job details to cache
                .expireAfterWrite(30, TimeUnit.MINUTES) // Cache entries expire after 30 minutes
                .recordStats() // Enable statistics
                .build();
    }
}
