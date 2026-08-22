package com.darb.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;
import java.util.Set;

public class ValidTimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

    private static final Set<String> CANONICAL_AREA_PREFIXES = Set.of(
            "Africa/", "America/", "Antarctica/", "Arctic/", "Asia/",
            "Atlantic/", "Australia/", "Europe/", "Indian/", "Pacific/", "Etc/");
    private static final Set<String> CANONICAL_SPECIAL_IDS = Set.of("UTC", "GMT");

    private final Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (!availableZoneIds.contains(value)) {
            return false;
        }
        // ZoneId preserves tzdb link IDs (e.g. "US/Eastern"), so getId() equality
        // cannot tell links apart from canonical zones. Canonical zones live under
        // a continent area or are one of the special IDs like UTC.
        return CANONICAL_SPECIAL_IDS.contains(value)
                || CANONICAL_AREA_PREFIXES.stream().anyMatch(value::startsWith);
    }
}
