package com.rwdenmark.x12.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness probe for the portfolio's Live Demo failover. Returns 200 so the check can
 * confirm the app is up. No database here, so there is nothing to warm. CORS for /api/**
 * is already configured in WebConfig (portfolio origin and the Tailscale Funnel host).
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
