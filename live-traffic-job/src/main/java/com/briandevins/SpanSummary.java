package com.briandevins;

import java.io.Serializable;

public class SpanSummary implements Serializable {
    static final String DEFAULT_FROM = "INTERNET";
    static final String DEFAULT_TO = "UNKNOWN";

    String traceId;
    String spanId;

    long timestamp;
    long duration;

    String to = DEFAULT_TO;
    String from = DEFAULT_FROM;

    String env;
    String region;
}
