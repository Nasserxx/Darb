package com.darb.controllers.v1;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@Tag(name = "Health", description = "System health and readiness endpoints")
@SecurityRequirements()
public class HealthController {

	@GetMapping("/health")
	@Operation(summary = "Health check", description = "Returns service liveness status.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Service is healthy")
	})
	public Map<String, String> health() {
		log.debug("Health check invoked");
		return Map.of("status", "UP");
	}

}
