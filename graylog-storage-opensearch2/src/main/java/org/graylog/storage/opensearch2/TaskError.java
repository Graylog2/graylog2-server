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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskError(@JsonProperty("type") String type, @JsonProperty("reason") String reason,
                        @Nullable @JsonProperty("caused_by") String causedBy) {

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("type='").append(type).append('\'');
        sb.append(", reason='").append(reason).append('\'');
        if (causedBy != null) {
            sb.append(", causedBy='").append(causedBy).append('\'');
        }
        return sb.toString();
    }
}
