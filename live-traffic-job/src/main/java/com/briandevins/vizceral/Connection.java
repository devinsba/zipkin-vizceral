package com.briandevins.vizceral;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Connection {
    private String source;
    private String target;

    private String clazz;

    private Metrics metrics;

    private Connection() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String source;
        private String target;
        private String clazz;
        private Metrics metrics;

        private Builder() {
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder clazz(String clazz) {
            this.clazz = clazz;
            return this;
        }

        public Builder metrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Connection build() {
            Connection connection = new Connection();
            connection.target = this.target;
            connection.clazz = this.clazz;
            connection.source = this.source;
            connection.metrics = this.metrics;
            return connection;
        }
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    @JsonProperty("class")
    public String getClazz() {
        return clazz;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
