package com.csa.official.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页参数收敛的边界测试。
 *
 * <p>这些用例守的是一个具体风险：分页接口如果原样信任前端传来的 size，
 * 一个 {@code ?size=1000000} 就能让数据库全表返回、把堆打满。
 */
class PageUtilsTest {

    @Test
    void clampsOversizedPageSizeToMax() {
        Page<Object> page = PageUtils.of(1, 1_000_000);
        assertThat(page.getSize()).isEqualTo(PageUtils.MAX_PAGE_SIZE);
    }

    @Test
    void usesDefaultPageSizeWhenSizeIsNull() {
        Page<Object> page = PageUtils.of(1, null);
        assertThat(page.getSize()).isEqualTo(PageUtils.DEFAULT_PAGE_SIZE);
    }

    @Test
    void rejectsNonPositivePageSize() {
        assertThat(PageUtils.of(1, 0).getSize()).isEqualTo(1);
        assertThat(PageUtils.of(1, -5).getSize()).isEqualTo(1);
    }

    @Test
    void normalizesInvalidPageNumberToFirstPage() {
        assertThat(PageUtils.of(null, 10).getCurrent()).isEqualTo(1);
        assertThat(PageUtils.of(0, 10).getCurrent()).isEqualTo(1);
        assertThat(PageUtils.of(-3, 10).getCurrent()).isEqualTo(1);
    }

    @Test
    void keepsValidPageAndSizeUntouched() {
        Page<Object> page = PageUtils.of(3, 25);
        assertThat(page.getCurrent()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(25);
    }

    @Test
    void clampLimitHonoursDefaultAndUpperBound() {
        assertThat(PageUtils.clampLimit(null, 100)).isEqualTo(100);
        assertThat(PageUtils.clampLimit(5, 100)).isEqualTo(5);
        assertThat(PageUtils.clampLimit(99_999, 100)).isEqualTo(PageUtils.MAX_LIST_LIMIT);
        assertThat(PageUtils.clampLimit(0, 100)).isEqualTo(1);
        assertThat(PageUtils.clampLimit(-1, 100)).isEqualTo(1);
    }
}
