package com.csa.official.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数收敛工具。
 *
 * <p>存在的意义：分页接口如果直接把前端传来的 {@code size} 丢给 MyBatis-Plus，
 * 攻击者只要请求 {@code ?size=1000000} 就能让数据库一次性吐出全表并把堆打满。
 * 之前每个 Controller 各写各的 {@code Math.min(...)}，上限还不一致
 * （有的 200、有的干脆没有），所以统一收到这里。
 */
public final class PageUtils {

    /** 分页接口默认每页条数。 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 分页接口每页条数上限。 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 不分页的列表接口（如提案列表、通讯录）单次返回条数上限。 */
    public static final int MAX_LIST_LIMIT = 200;

    private PageUtils() {
    }

    /**
     * 构造一个页码和每页条数都已经收敛过的分页对象。
     *
     * @param page 前端传入的页码，null 或小于 1 时按第 1 页处理
     * @param size 前端传入的每页条数，null 时用 {@link #DEFAULT_PAGE_SIZE}，超过 {@link #MAX_PAGE_SIZE} 时截断
     */
    public static <T> Page<T> of(Integer page, Integer size) {
        long safePage = page == null || page < 1 ? 1L : page;
        int safeSize = size == null ? DEFAULT_PAGE_SIZE : clamp(size, 1, MAX_PAGE_SIZE);
        return new Page<>(safePage, safeSize);
    }

    /**
     * 收敛不分页列表接口的 limit，上限为 {@link #MAX_LIST_LIMIT}。
     *
     * @param limit        前端传入的条数，null 时用 defaultLimit
     * @param defaultLimit 默认条数
     */
    public static int clampLimit(Integer limit, int defaultLimit) {
        int raw = limit == null ? defaultLimit : limit;
        return clamp(raw, 1, MAX_LIST_LIMIT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
