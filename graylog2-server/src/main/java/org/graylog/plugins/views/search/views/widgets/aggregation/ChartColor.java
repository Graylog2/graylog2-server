package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonAutoDetect
public class ChartColor {
    private final String value;

    @JsonCreator
    public ChartColor(String value) {
        if (!value.startsWith("#") || !value.matches("#[a-f0-9]{3,6}")) {
            throw new IllegalArgumentException("Color value must begin with '#' and contain 3-6 hexadecimal values.");
        }
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return this.value;
    }
}
