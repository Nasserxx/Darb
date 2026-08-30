package com.darb.util;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class NameFilterSpecs {

    public static final int MAX_Q_LENGTH = 100;

    private NameFilterSpecs() {
    }

    public static String normalizeQ(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String trimmed = q.trim();
        if (trimmed.length() > MAX_Q_LENGTH) {
            return trimmed.substring(0, MAX_Q_LENGTH);
        }
        return trimmed;
    }

    public static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static String likePattern(String q) {
        String normalized = normalizeQ(q);
        if (normalized == null) {
            return null;
        }
        return "%" + escapeLike(normalized.toLowerCase(Locale.ROOT)) + "%";
    }

    public static <T> Specification<T> userJoinFullNameLike(String userJoinName, String q) {
        String pattern = likePattern(q);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) -> {
            var user = root.join(userJoinName, JoinType.INNER);
            return cb.like(cb.lower(user.get("fullName")), pattern, '\\');
        };
    }

    public static <T> Specification<T> parentUserFullNameLike(String q) {
        String pattern = likePattern(q);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("parent").get("fullName")), pattern, '\\');
    }

    public static <T> Specification<T> circleNameLike(String q) {
        return attributeLike("name", q);
    }

    public static <T> Specification<T> attributeLike(String attribute, String q) {
        String pattern = likePattern(q);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(attribute)), pattern, '\\');
    }

    public static <T> Specification<T> enrollmentStudentFullNameLike(String q) {
        String pattern = likePattern(q);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) -> {
            var student = root.join("student", JoinType.INNER);
            var user = student.join("user", JoinType.INNER);
            return cb.like(cb.lower(user.get("fullName")), pattern, '\\');
        };
    }

    public static <T> Specification<T> paymentStudentFullNameLike(String q) {
        String pattern = likePattern(q);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) -> {
            var student = root.join("student", JoinType.INNER);
            var user = student.join("user", JoinType.INNER);
            return cb.like(cb.lower(user.get("fullName")), pattern, '\\');
        };
    }

    public static <T> Specification<T> and(Specification<T> base, Specification<T> extra) {
        if (extra == null) {
            return base;
        }
        if (base == null) {
            return extra;
        }
        return base.and(extra);
    }
}
