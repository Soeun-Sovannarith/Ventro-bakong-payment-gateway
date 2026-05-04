package com.rith.ventro_bakong.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BakongTokenService {

    private static final Logger log = LoggerFactory.getLogger(BakongTokenService.class);

    // Optional pre-configured token from application.properties (fallback)
    @Value("${bakong.api.token:}")
    private String configuredToken;

    // Cached token and expiry (if you integrate a real token fetcher later you can set these)
    private String cachedToken;
    private Instant cachedExpiry;

    /**
     * Get a bearer token to call Bakong APIs.
     * Current implementation returns in order of precedence:
     * 1. Environment variable BAKONG_API_TOKEN (if present)
     * 2. application.properties value bakong.api.token (if present)
     * If neither is present this method throws an exception explaining what's missing.
     */
    public synchronized String getToken() {
        // If cached and not expired (5 seconds safety), return
        if (cachedToken != null && cachedExpiry != null && Instant.now().isBefore(cachedExpiry.minusSeconds(5))) {
            return cachedToken;
        }

        String env = System.getenv("BAKONG_API_TOKEN");
        if (env != null && !env.isBlank()) {
            log.debug("Using BAKONG_API_TOKEN from environment");
            cachedToken = env.trim();
            // We don't know expiry for env-provided token; set short cache window (5 minutes)
            cachedExpiry = Instant.now().plusSeconds(300);
            return cachedToken;
        }

        if (configuredToken != null && !configuredToken.isBlank()) {
            log.debug("Using bakong.api.token from application properties");
            cachedToken = configuredToken.trim();
            cachedExpiry = Instant.now().plusSeconds(300);
            return cachedToken;
        }

        throw new IllegalStateException("No Bakong API token configured. Set environment variable BAKONG_API_TOKEN or provide bakong.api.token in application.properties. \n" +
                "Note: Production integrations should implement an actual token fetch/refresh flow in BakongTokenService to obtain short-lived bearer tokens.");
    }
}

