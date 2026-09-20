package com.csa.official.common.cache;

/**
 * Stable cache keys used as an application protocol.
 *
 * <p>Changing a DTO shape requires bumping {@link #VERSION}; old values must
 * never be deserialized as a newer contract.</p>
 */
public final class CacheKeys {

    public static final String VERSION = "v2";

    private CacheKeys() {
    }

    public static String generation(String scope) {
        return "csa:public:" + scope + ":generation:" + VERSION;
    }

    public static String about() {
        return "csa:public:about:" + VERSION;
    }

    public static String carousel() {
        return "csa:public:carousel:active:" + VERSION;
    }

    public static String competitionList(int page, int size) {
        return "csa:public:competition:list:" + page + ":" + size + ":public:" + VERSION;
    }

    public static String competitionDetail(long id) {
        return "csa:public:competition:detail:" + id + ":" + VERSION;
    }

    public static String competitionDetailNull(long id) {
        return "csa:public:competition:detail:null:" + id + ":" + VERSION;
    }

    public static String competitionDetailLock(long id) {
        return "csa:lock:competition:detail:" + id;
    }
}
