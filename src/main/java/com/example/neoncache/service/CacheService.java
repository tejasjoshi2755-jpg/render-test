package com.example.neoncache.service;

import com.example.neoncache.dto.CacheResponse;
import com.example.neoncache.entity.RequestLog;
import com.example.neoncache.repository.RequestLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {

    private static final String CACHE_KEY = "12345";

    /*
     * L1 cache:
     * key   = 12345
     * value = ddMMyyHHmmSS
     *
     * static means all CacheService instances in the same JVM
     * share the same cache.
     */
    private static final ConcurrentHashMap<String, String> L1_CACHE =
            new ConcurrentHashMap<>();

    private final RequestLogRepository requestLogRepository;

    public CacheService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    public CacheResponse getValue(String request) {

        if (request == null) {
            throw new IllegalArgumentException("request.value must not be empty");
        }

        // STEP 1: Store in DB FIRST.
        RequestLog requestLog = new RequestLog(
        		request,
                LocalDateTime.now()
        );
        requestLogRepository.save(requestLog);

        // STEP 2: Check L1 cache.
        String cachedValue = L1_CACHE.get(CACHE_KEY);

        if (cachedValue != null && !cachedValue.isBlank()) {
            return new CacheResponse(CACHE_KEY, cachedValue, "L1_CACHE");
        }

        // STEP 3: L1 is empty -> call send() logic.
        return send();
    }

    /*
     * Endpoint-2 business logic.
     *
     * Clears L1 and stores the current timestamp using:
     * ddMMyyHHmmss
     */
    public synchronized CacheResponse send() {

        // Clear L1 cache
        L1_CACHE.clear();

        // Get total count from DB
        long totalDbCount = requestLogRepository.count();

        // Store DB count in L1 cache
        String currentValue = String.valueOf(totalDbCount);

        L1_CACHE.put(CACHE_KEY, currentValue);

        return new CacheResponse(
                CACHE_KEY,
                currentValue,
                "SEND"
        );
    }

    // Useful for testing/debugging.
    public static String getL1Value() {
        return L1_CACHE.get(CACHE_KEY);
    }
}
