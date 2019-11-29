package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    Interpolation(String value) {
        this.value = value;
    }
}
