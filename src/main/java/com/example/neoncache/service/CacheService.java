
package com.example.neoncache.service;

import com.example.neoncache.dto.CacheResponse;
import com.example.neoncache.entity.RequestLog;
import com.example.neoncache.repository.RequestLogRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {

    private static final Logger log =
            LoggerFactory.getLogger(CacheService.class);

    private static final String CACHE_KEY = "12345";

    private static final ConcurrentHashMap<String, String> L1_CACHE =
            new ConcurrentHashMap<>();

    private final RequestLogRepository requestLogRepository;

    public CacheService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    private CacheResponse dbDown() {
        log.warn("Database unavailable. Returning DB Down response.");

        return new CacheResponse(
                CACHE_KEY,
                "DB Down",
                "DB_DOWN"
        );
    }

    public CacheResponse getValue(
            String request,
            HttpServletRequest req) {

        if (request == null || request.isBlank()) {
            log.warn("Invalid cache request: empty value");
            throw new IllegalArgumentException(
                    "request.value must not be empty");
        }

        String ip = req.getRemoteAddr();

        log.info("Cache request received. Client IP: {}", ip);

        // STEP 1: Save request in database
        try {
            RequestLog requestLog = new RequestLog(
                    request,
                    LocalDateTime.now()
            );

            requestLogRepository.save(requestLog);

            log.info("Database INSERT successful");

        } catch (DataAccessException e) {

            log.error("DATABASE INSERT FAILED: {}", e.getMessage(), e);

            return dbDown();
        }

        // STEP 2: Check L1 cache
        String cachedValue = L1_CACHE.get(CACHE_KEY);

        if (cachedValue != null && !cachedValue.isBlank()) {

            log.info("L1 CACHE HIT");

            return new CacheResponse(
                    CACHE_KEY,
                    cachedValue + "-" + ip,
                    "L1_CACHE"
            );
        }

        log.info("L1 CACHE MISS");

        // STEP 3: Fetch database count
        return send(ip);
    }

    public synchronized CacheResponse send(String ip) {

        log.info("SEND operation started");

        final long totalDbCount;

        try {

            totalDbCount = requestLogRepository.count();

            log.info("DATABASE COUNT successful: {}", totalDbCount);

        } catch (DataAccessException e) {

            log.error("DATABASE COUNT FAILED: {}", e.getMessage(), e);

            return dbDown();
        }

        String currentValue = String.valueOf(totalDbCount);

        L1_CACHE.put(CACHE_KEY, currentValue);

        log.info("L1 cache updated: {}", currentValue);

        return new CacheResponse(
                CACHE_KEY,
                currentValue + "-" + ip,
                "SEND"
        );
    }

    public String clearCache() {

        log.info("Manual L1 cache clear requested");

        L1_CACHE.clear();

        return "Cache Clear";
    }

    public static String getL1Value() {
        return L1_CACHE.get(CACHE_KEY);
    }
}
