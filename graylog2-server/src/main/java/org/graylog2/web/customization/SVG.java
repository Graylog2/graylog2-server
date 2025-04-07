package org.graylog2.web.customization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class SVG {
    private final String data;

    @JsonCreator
    public SVG(String data) {
        if (!data.startsWith("<svg") && !data.startsWith("<?xml")) {
            throw new IllegalArgumentException("Invalid SVG data supplied: " + data);
        }

        this.data = data;
    }

    @JsonValue
    public String data() {
        return data;
    }
}
