package com.csa.official.common.observability;

import org.slf4j.MDC;

public final class TraceContext {

    public static final String MDC_KEY = "traceId";
    public static final String REQUEST_ATTRIBUTE = TraceContext.class.getName() + ".traceId";
    public static final String REQUEST_HEADER = "X-Request-ID";

    private TraceContext() {
    }

    public static String currentTraceId() {
        return MDC.get(MDC_KEY);
    }
}
