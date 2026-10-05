package com.example.neoncache.dto;

public record CacheResponse(
        String key,
        String value,
        String source
) {
}
