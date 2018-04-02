package com.briandevins;

import com.briandevins.vizceral.Connection;
import com.briandevins.vizceral.Metrics;
import com.briandevins.vizceral.RendererType;
import com.briandevins.vizceral.Node;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SpanSummaryRegistry {

    private static final AtomicReference<Node> NODE = new AtomicReference<>(Node.newBuilder().build());

    private static final Node INTERNET = Node.newBuilder()
            .renderer(RendererType.REGION).name("INTERNET").displayName("INTERNET").clazz("normal").build();

    static void update(List<SpanSummary> spanSummaries) {
        Map<String, Map<String, Map<String, Cluster>>> clustersByRegion = getClusters(spanSummaries);

        NODE.set(Node.newBuilder()
                .name("root")
                .renderer(RendererType.GLOBAL)
                .updated(System.currentTimeMillis())
                .nodes(getRegionNodes(clustersByRegion))
                .connections(connectionsForGlobalView(clustersByRegion))
                .build());
    }

    static Node getNode() {
        return NODE.get();
    }

    private static Map<String, Map<String, Map<String, Cluster>>> getClusters(List<SpanSummary> spanSummaries) {
        Map<String, Map<String, Map<String, Cluster>>> clusters = new HashMap<>();

        spanSummaries.stream().map(s -> s.region).filter(Objects::nonNull).forEach(r -> clusters.put(r, new HashMap<>())); // create the maps by region
        spanSummaries.stream().map(s -> s.env).filter(Objects::nonNull).forEach(e -> { // create the maps by env in the region maps
            for (String region : clusters.keySet()) {
                clusters.get(region).put(e, new HashMap<>());
            }
        });

        spanSummaries.stream().filter(s -> s.env != null && s.region != null).forEach(s -> {
            Cluster cluster = clusters.get(s.region).get(s.env).get(s.to);
            if (cluster == null) {
                cluster = new Cluster();
                clusters.get(s.region).get(s.env).put(s.to, cluster);

                cluster.region = s.region;
                cluster.env = s.env;
                cluster.app = s.to;
            }

            Metrics metrics = cluster.connectionsFrom.get(s.from);
            if (metrics == null) {
                metrics = new Metrics(Job.WINDOW_LENGTH_SECONDS);
                cluster.connectionsFrom.put(s.from, metrics);
            }
            metrics.incNormal();
        });

        return clusters;
    }

    private static List<Node> getRegionNodes(Map<String, Map<String, Map<String, Cluster>>> clusters) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(INTERNET);

        for (String region : clusters.keySet()) {
            nodes.add(Node.newBuilder()
                    .renderer(RendererType.GLOBAL)
                    .displayName(region)
                    .name(region)
                    .clazz("normal")
                    .connections(connectionsForRegionView(clusters.get(region)))
                    .nodes(getEnvNodes(clusters.get(region)))
                    .build());
        }

        return nodes;
    }

    private static List<Node> getEnvNodes(Map<String, Map<String, Cluster>> clustersInRegion) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(INTERNET);

        for (String env : clustersInRegion.keySet()) {
            nodes.add(Node.newBuilder()
                    .renderer(RendererType.REGION)
                    .displayName(env)
                    .name(env)
                    .clazz("normal")
                    .connections(connectionsForEnvView(clustersInRegion.get(env)))
                    .nodes(getClusterNodes(clustersInRegion.get(env)))
                    .build());
        }

        return nodes;
    }

    private static List<Node> getClusterNodes(Map<String, Cluster> clusters) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(INTERNET);

        for (String app : clusters.keySet()) {
            nodes.add(Node.newBuilder()
                    .renderer(RendererType.REGION)
                    .displayName(app)
                    .name(app)
                    .clazz("normal")
                    .build());
        }

        return nodes;
    }

    private static List<Connection> connectionsForGlobalView(Map<String, Map<String, Map<String, Cluster>>> clusters) {
        List<Connection> connections = new ArrayList<>();

        for (Map.Entry<String, Map<String, Map<String, Cluster>>> clustersWithRegion : clusters.entrySet()) {
            List<Connection> connectionsForRegion = new ArrayList<>();

            clustersWithRegion.getValue().values().forEach(c -> connectionsForRegion.addAll(connectionsForRegionView(clustersWithRegion.getValue())));
            List<Metrics> metricsList = connectionsForRegion.stream().map(Connection::getMetrics).collect(Collectors.toList());
            Metrics metrics = consolidateMetrics(metricsList);

            connections.add(Connection.newBuilder()
                    .source(SpanSummary.DEFAULT_FROM)
                    .target(clustersWithRegion.getKey())
                    .metrics(metrics)
                    .clazz("normal")
                    .build()
            );
        }

        return connections;
    }

    /*
     * Region view shows the envs in a region
     */
    private static List<Connection> connectionsForRegionView(Map<String, Map<String, Cluster>> clustersInRegion) {
        List<Connection> connections = new ArrayList<>();

        for (Map.Entry<String, Map<String, Cluster>> clustersWithEnv : clustersInRegion.entrySet()) {
            List<Connection> connectionsForEnv = new ArrayList<>();
            clustersWithEnv.getValue().values().forEach(c -> connectionsForEnv.addAll(connectionsForCluster(c)));
            List<Metrics> metricsList = connectionsForEnv.stream().map(Connection::getMetrics).collect(Collectors.toList());
            Metrics metrics = consolidateMetrics(metricsList);

            connections.add(Connection.newBuilder()
                    .source(SpanSummary.DEFAULT_FROM)
                    .target(clustersWithEnv.getKey())
                    .metrics(metrics)
                    .clazz("normal")
                    .build()
            );
        }

        return connections;
    }

    /*
     * Env view is showing the apps in a environment
     */
    private static List<Connection> connectionsForEnvView(Map<String, Cluster> clustersInEnv) {
        List<Connection> connections = new ArrayList<>();

        for (Map.Entry<String, Cluster> cluster : clustersInEnv.entrySet()) {
            connections.addAll(connectionsForCluster(cluster.getValue()));
        }

        return connections;
    }

    /*
     * The connections to an app in the smallest view
     */
    private static List<Connection> connectionsForCluster(Cluster cluster) {
        List<Connection> connections = new ArrayList<>();
        for (Map.Entry<String, Metrics> metrics : cluster.connectionsFrom.entrySet()) {
            connections.add(Connection.newBuilder()
                    .source(metrics.getKey())
                    .target(cluster.app)
                    .metrics(metrics.getValue())
                    .clazz("normal")
                    .build()
            );
        }
        return connections;
    }

    private static Metrics consolidateMetrics(List<Metrics> metrics) {
        Metrics result = new Metrics(Job.WINDOW_LENGTH_SECONDS);
        for (Metrics m : metrics) {
            result.incNormal(m.getNormalCount());
        }
        return result;
    }
}
