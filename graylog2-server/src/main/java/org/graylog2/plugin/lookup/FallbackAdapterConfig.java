/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is the dummy config that accepts anything and has a marker method to detect a missing plugin.
 * Otherwise loading the config from the database fails hard.
 */
@JsonAutoDetect
public class FallbackAdapterConfig implements LookupDataAdapterConfiguration {

    @JsonProperty
    private String type = "FallbackAdapterConfig";

    @Override
    public String type() {
        return type;
    }

    @JsonAnySetter
    public void setType(String key, Object value) {
        // we ignore all the other values, we only want to be able to deserialize unknown configs
    }
}
