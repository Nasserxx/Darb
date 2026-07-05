package com.darb.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void rejectsShortSecret() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "jwtSecret", "short");
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(svc, "validateSecret"));
    }

    @Test
    void rejectsBlankSecret() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "jwtSecret", "   ");
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(svc, "validateSecret"));
    }
}
