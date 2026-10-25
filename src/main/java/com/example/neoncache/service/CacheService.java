
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

    public CacheResponse getValue(
            String request,
            HttpServletRequest req) {

        if (request == null || request.isBlank()) {
            log.warn("Cache request rejected: request.value is empty");
            throw new IllegalArgumentException(
                    "request.value must not be empty");
        }

        String ip = req.getRemoteAddr();

        log.info("Cache request received. Client IP: {}", ip);

        // STEP 1: Save request in database first.
        try {
            RequestLog requestLog = new RequestLog(
                    request,
                    LocalDateTime.now()
            );

            requestLogRepository.save(requestLog);

            log.info("Database INSERT successful for RequestLog");

        } catch (DataAccessException e) {
            log.error(
                    "DATABASE INSERT FAILED: Could not save RequestLog. " +
                    "Check Neon availability, JDBC connection, schema, " +
                    "constraints, and SQL errors.",
                    e
            );
            throw e;

        } catch (RuntimeException e) {
            log.error(
                    "UNEXPECTED ERROR during RequestLog save operation",
                    e
            );
            throw e;
        }

        // STEP 2: Check L1 cache.
        String cachedValue = L1_CACHE.get(CACHE_KEY);

        if (cachedValue != null && !cachedValue.isBlank()) {
            log.info("L1 CACHE HIT. Returning cached value.");

            return new CacheResponse(
                    CACHE_KEY,
                    cachedValue + "-" + ip,
                    "L1_CACHE"
            );
        }

        log.info("L1 CACHE MISS. Fetching database count.");

        // STEP 3: Cache miss -> execute SEND logic.
        return send(ip);
    }

    /*
     * Clears L1, counts database records, and caches the count.
     */
    public synchronized CacheResponse send(String ip) {

        log.info("SEND operation started");

        L1_CACHE.clear();
        log.debug("L1 cache cleared");

        final long totalDbCount;

        try {
            totalDbCount = requestLogRepository.count();

            log.info(
                    "DATABASE COUNT successful. Total RequestLog records: {}",
                    totalDbCount
            );

        } catch (DataAccessException e) {
            log.error(
                    "DATABASE COUNT FAILED: Could not execute " +
                    "requestLogRepository.count(). Check Neon compute " +
                    "allowance, branch status, database connectivity, " +
                    "credentials, and SQL errors.",
                    e
            );
            throw e;

        } catch (RuntimeException e) {
            log.error(
                    "UNEXPECTED ERROR during database COUNT operation",
                    e
            );
            throw e;
        }

        String currentValue = String.valueOf(totalDbCount);

        L1_CACHE.put(CACHE_KEY, currentValue);

        log.info("L1 cache updated successfully. Key: {}", CACHE_KEY);
        log.info("SEND operation completed successfully");

        return new CacheResponse(
                CACHE_KEY,
                currentValue + "-" + ip,
                "SEND"
        );
    }

    public String clearCache() {
        log.info("Manual L1 cache clear requested");

        L1_CACHE.clear();

        log.info("L1 cache cleared successfully");

        return "Cache Clear";
    }

    public static String getL1Value() {
        return L1_CACHE.get(CACHE_KEY);
    }
}
