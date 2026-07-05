package com.darb.configs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Dev-only: if default port 8080 is busy, bind to the first free port in 8081–8099.
 */
public class DevServerPortFallback implements EnvironmentPostProcessor {

    private static final int DEFAULT_PORT = 8080;
    private static final int FALLBACK_START = 8081;
    private static final int FALLBACK_END = 8099;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String profiles = firstNonBlank(
                environment.getProperty("spring.profiles.active"),
                environment.getProperty("SPRING_PROFILES_ACTIVE"),
                "dev");
        if (Arrays.stream(profiles.split(",")).map(String::trim).noneMatch("dev"::equals)) {
            return;
        }
        int configuredPort = environment.getProperty("SERVER_PORT", Integer.class,
                environment.getProperty("server.port", Integer.class, DEFAULT_PORT));
        if (configuredPort != DEFAULT_PORT || isAvailable(DEFAULT_PORT)) {
            return;
        }
        int port = firstAvailable(FALLBACK_START, FALLBACK_END);
        if (port < 0) {
            throw new IllegalStateException(
                    "Port %d in use and no free port in %d–%d".formatted(DEFAULT_PORT, FALLBACK_START, FALLBACK_END));
        }
        environment.getPropertySources().addFirst(new MapPropertySource(
                "devPortFallback", Map.of("server.port", port)));
        System.err.printf("Port %d in use — starting on %d (set SERVER_PORT to pin a port)%n", DEFAULT_PORT, port);
    }

    private static boolean isAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int firstAvailable(int start, int end) {
        for (int port = start; port <= end; port++) {
            if (isAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "dev";
    }
}
