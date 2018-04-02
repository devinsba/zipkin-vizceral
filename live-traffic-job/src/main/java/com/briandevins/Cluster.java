package com.briandevins;

import com.briandevins.vizceral.Metrics;

import java.util.HashMap;
import java.util.Map;

public class Cluster {
    String app;
    String region;
    String env;

    Map<String, Metrics> connectionsFrom = new HashMap<>();
}
