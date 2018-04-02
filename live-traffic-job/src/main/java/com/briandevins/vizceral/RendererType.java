package com.briandevins.vizceral;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RendererType {
    GLOBAL("global"),
    REGION("region");

    private String value;

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    RendererType(String value) {
        this.value = value;
    }
}
