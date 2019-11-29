package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Interpolation {
    linear("linear"),
    stepAfter("step-after"),
    spline("spline");

    private final String value;

    @JsonValue
    public String value() {
        return this.value;
    }

    public static Interpolation defaultValue() {
        return linear;
    }

    public static Interpolation fromLegacyValue(String legacyValue) {
        switch (legacyValue) {
            case "linear": return linear;
            case "step-after": return stepAfter;
            case "cardinal":
            case "basis":
            case "bundle":
            case "monotone": return spline;
        }
        throw new RuntimeException("Invalid interpolation value: " + legacyValue);
    }

    Interpolation(String value) {
        this.value = value;
    }
}
