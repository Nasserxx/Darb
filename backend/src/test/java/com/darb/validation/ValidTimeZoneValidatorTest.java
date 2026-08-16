package com.darb.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidTimeZoneValidatorTest {

    static Validator validator;

    static class Carrier {
        @ValidTimeZone
        String timezone;
    }

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsCanonicalIanaZones() {
        assertThat(validator.validate(carrier("Asia/Riyadh"))).isEmpty();
        assertThat(validator.validate(carrier("Europe/Berlin"))).isEmpty();
        assertThat(validator.validate(carrier("UTC"))).isEmpty();
    }

    @Test
    void acceptsBlank() {
        assertThat(validator.validate(carrier(""))).isEmpty();
        assertThat(validator.validate(carrier(null))).isEmpty();
    }

    @Test
    void rejectsAliasesAndOffsets() {
        assertThat(validator.validate(carrier("US/Eastern"))).isNotEmpty();
        assertThat(validator.validate(carrier("+02:00"))).isNotEmpty();
        assertThat(validator.validate(carrier("EST"))).isNotEmpty();
        assertThat(validator.validate(carrier("not-a-zone"))).isNotEmpty();
    }

    private static Carrier carrier(String value) {
        Carrier c = new Carrier();
        c.timezone = value;
        return c;
    }
}
