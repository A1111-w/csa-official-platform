package com.csa.official.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNormalizerTest {

    @Test
    void normalizesEmailCaseAndStudentIdWhitespace() {
        assertThat(AccountNormalizer.email("  Student@Example.EDU  ")).isEqualTo("student@example.edu");
        assertThat(AccountNormalizer.studentId("  st-2026-01  ")).isEqualTo("ST-2026-01");
    }
}
