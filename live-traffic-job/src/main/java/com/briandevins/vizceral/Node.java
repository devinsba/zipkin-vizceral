package com.briandevins.vizceral;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Node {
    private String name;
    private RendererType renderer;
    private String displayName;

    private List<Node> nodes;

    private List<Connection> connections;

    private long updated;

    private String clazz;

    private Node() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private RendererType renderer;
        private String displayName;
        private List<Node> nodes;
        private List<Connection> connections;
        private long updated = System.currentTimeMillis();
        private String clazz;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder renderer(RendererType renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder nodes(List<Node> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder connections(List<Connection> connections) {
            this.connections = connections;
            return this;
        }

        public Builder updated(long updated) {
            this.updated = updated;
            return this;
        }

        public Builder clazz(String clazz) {
            this.clazz = clazz;
            return this;
        }

        public Node build() {
            Node node = new Node();
            node.renderer = this.renderer;
            node.name = this.name;
            node.displayName = this.displayName;
            node.nodes = this.nodes;
            node.connections = this.connections;
            node.updated = this.updated;
            node.clazz = this.clazz;
            return node;
        }
    }

    public String getName() {
        return name;
    }

    public RendererType getRenderer() {
        return renderer;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public long getUpdated() {
        return updated;
    }

    @JsonProperty("class")
    public String getClazz() {
        return clazz;
    }
}
