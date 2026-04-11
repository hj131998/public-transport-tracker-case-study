package com.tracker.cache;

import java.util.Optional;

public interface CacheService {
    <T> void put(String key, T value);
    <T> Optional<T> get(String key, Class<T> type);
    <T> Optional<T> getStale(String key, Class<T> type);
    void evict(String key);
    int size();
}
