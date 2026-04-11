package com.tracker.cache;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
public class CacheEntry<T> {

    private final T data;
    private final Instant createdAt;

    public CacheEntry(T data) {
        this.data = data;
        this.createdAt = Instant.now();
    }

    public boolean isExpired(int ttlSeconds) {
        return Duration.between(createdAt, Instant.now()).getSeconds() >= ttlSeconds;
    }

    public long ageSeconds() {
        return Duration.between(createdAt, Instant.now()).getSeconds();
    }
}
