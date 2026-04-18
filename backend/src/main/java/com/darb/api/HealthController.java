package com.darb.api;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {

	@GetMapping("/health")
	public Map<String, String> health() {
		log.debug("Health check invoked");
		return Map.of("status", "UP");
	}

}
