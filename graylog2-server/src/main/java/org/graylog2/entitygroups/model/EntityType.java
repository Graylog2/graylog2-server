package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

public enum EntityType {
    @JsonProperty("assets")
    ASSETS,
    @JsonProperty("sigma_rules")
    SIGMA_RULES;

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EntityType valueOfIgnoreCase(String name) {
        return EntityType.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
