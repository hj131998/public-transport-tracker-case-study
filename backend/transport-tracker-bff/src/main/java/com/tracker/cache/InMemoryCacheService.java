package com.tracker.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
public class InMemoryCacheService implements CacheService {

    private final int ttlSeconds;
    private final int staleTtlSeconds;
    private final int maxSize;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // LinkedHashMap with access-order = true gives LRU behaviour
    private final Map<String, CacheEntry<?>> store;

    public InMemoryCacheService(
            @Value("${app.cache.ttl-seconds:30}") int ttlSeconds,
            @Value("${app.cache.stale-ttl-seconds:300}") int staleTtlSeconds,
            @Value("${app.cache.max-size:1000}") int maxSize) {
        this.ttlSeconds = ttlSeconds;
        this.staleTtlSeconds = staleTtlSeconds;
        this.maxSize = maxSize;
        this.store = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<?>> eldest) {
                return size() > maxSize;
            }
        };
    }

    @Override
    public <T> void put(String key, T value) {
        lock.writeLock().lock();
        try {
            store.put(key, new CacheEntry<>(value));
            log.debug("Cache PUT key={}", key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        lock.readLock().lock();
        try {
            CacheEntry<?> entry = store.get(key);
            if (entry == null) {
                log.debug("Cache MISS key={}", key);
                return Optional.empty();
            }
            if (entry.isExpired(ttlSeconds)) {
                log.debug("Cache EXPIRED key={} age={}s", key, entry.ageSeconds());
                return Optional.empty();
            }
            log.debug("Cache HIT key={} age={}s", key, entry.ageSeconds());
            return Optional.of(type.cast(entry.getData()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getStale(String key, Class<T> type) {
        lock.readLock().lock();
        try {
            CacheEntry<?> entry = store.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            if (entry.isExpired(staleTtlSeconds)) {
                log.debug("Stale cache also expired key={} age={}s", key, entry.ageSeconds());
                return Optional.empty();
            }
            log.info("Serving STALE cache key={} age={}s", key, entry.ageSeconds());
            return Optional.of(type.cast(entry.getData()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void evict(String key) {
        lock.writeLock().lock();
        try {
            store.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return store.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Scheduled(fixedDelayString = "${app.cache.stale-ttl-seconds:300}000")
    public void evictExpired() {
        lock.writeLock().lock();
        try {
            int before = store.size();
            store.entrySet().removeIf(e -> e.getValue().isExpired(staleTtlSeconds));
            log.debug("Cache eviction: removed {} entries, remaining={}", before - store.size(), store.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
}
