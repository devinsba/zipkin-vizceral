package com.briandevins.vizceral;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Metrics {
    private int normalCount = 0;
    private int dangerCount = 0;
    private int warningCount = 0;

    private float divisor;

    public Metrics(int divisor) {
        this.divisor = divisor + 0.0f;
    }

    public void incNormal() {
        normalCount++;
    }

    public void incNormal(int count) {
        normalCount += count;
    }

    public float getNormal() {
        return normalCount / divisor;
    }

    @JsonIgnore
    public int getNormalCount() {
        return normalCount;
    }

    public float getDanger() {
        return dangerCount / divisor;
    }

    public float getWarning() {
        return warningCount / divisor;
    }
}
