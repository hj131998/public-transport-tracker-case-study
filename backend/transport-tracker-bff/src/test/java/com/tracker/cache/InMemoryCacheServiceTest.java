package com.tracker.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCacheServiceTest {

    private InMemoryCacheService cache;

    @BeforeEach
    void setUp() {
        // ttl=1s, staleTtl=3s, maxSize=5
        cache = new InMemoryCacheService(1, 3, 5);
    }

    @Test
    @DisplayName("Should return value immediately after put")
    void shouldReturnValueAfterPut() {
        cache.put("key1", "value1");
        Optional<String> result = cache.get("key1", String.class);
        assertThat(result).isPresent().contains("value1");
    }

    @Test
    @DisplayName("Should return empty on cache miss")
    void shouldReturnEmptyOnMiss() {
        Optional<String> result = cache.get("missing", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty after TTL expires")
    void shouldExpireAfterTtl() throws InterruptedException {
        cache.put("key2", "value2");
        Thread.sleep(1100);
        Optional<String> result = cache.get("key2", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return stale value after TTL but within stale TTL")
    void shouldReturnStaleAfterTtlExpiry() throws InterruptedException {
        cache.put("key3", "stale-value");
        Thread.sleep(1100);
        Optional<String> result = cache.getStale("key3", String.class);
        assertThat(result).isPresent().contains("stale-value");
    }

    @Test
    @DisplayName("Should return empty from stale after stale TTL expires")
    void shouldReturnEmptyAfterStaleTtlExpiry() throws InterruptedException {
        cache.put("key4", "old-value");
        Thread.sleep(3100);
        Optional<String> result = cache.getStale("key4", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should evict entry on explicit evict call")
    void shouldEvictEntry() {
        cache.put("key5", "value5");
        cache.evict("key5");
        assertThat(cache.get("key5", String.class)).isEmpty();
    }

    @Test
    @DisplayName("Should not exceed max size — LRU eviction")
    void shouldNotExceedMaxSize() {
        for (int i = 0; i < 6; i++) {
            cache.put("key" + i, "value" + i);
        }
        assertThat(cache.size()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should report correct size")
    void shouldReportCorrectSize() {
        cache.put("a", "1");
        cache.put("b", "2");
        assertThat(cache.size()).isEqualTo(2);
    }
}
