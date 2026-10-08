package com.example.neoncache.controller;

import com.example.neoncache.service.CacheService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

	private final CacheService cacheService;

	public CacheController(CacheService cacheService) {	
		this.cacheService = cacheService;
	}

	/**
	 * Endpoint 1: 1. Store request in DB first. 2. Check L1 cache for key 12345. 3.
	 * If present, return the cached value. 4. If absent, execute send() which
	 * clears L1 and stores current ddMMyyHHmmSS.
	 */
	@PostMapping("/get")
	public String getValue(@RequestBody String request, HttpServletRequest req) {
		return ResponseEntity.ok(cacheService.getValue(request, req)).getBody().value();
	}

	/**
	 * Endpoint 2: Clear L1 cache and store the current ddMMyyHHmmSS under key
	 * 12345.
	 */
	@PostMapping("/send")
	public String send() {
		return cacheService.clearCache();
	}
}
