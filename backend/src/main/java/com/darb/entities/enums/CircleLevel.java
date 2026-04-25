package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Difficulty or progression level of a study circle.
 */
@Getter
@RequiredArgsConstructor
public enum CircleLevel {
    BEGINNER("beginner", "Starting Quran reading"),
    INTERMEDIATE("intermediate", "Basic recitation and tajweed"),
    ADVANCED("advanced", "Advanced tajweed and memorization"),
    MEMORIZATION("memorization", "Full Quran memorization (Hifz)"),
    IJAZAH("ijazah", "Ijazah certification level");

    private final String key;
    private final String description;
}
