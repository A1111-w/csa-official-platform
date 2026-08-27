package com.csa.official.common.util;

import java.util.Locale;

public final class AccountNormalizer {

    private AccountNormalizer() {
    }

    public static String email(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String studentId(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public static String username(String value) {
        return value == null ? null : value.trim();
    }
}
