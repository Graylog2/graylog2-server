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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Cluster-wide defaults for Beats inputs. Applies to all Beats inputs that don't override the setting themselves.
 */
@JsonAutoDetect
@AutoValue
public abstract class BeatsInputDefaultsConfig {
    public static final String FIELD_MAX_EVENT_SIZE = "max_event_size";

    public static final int DEFAULT_MAX_EVENT_SIZE = 16 * 1024 * 1024;

    public static final BeatsInputDefaultsConfig DEFAULT_VALUES = create(DEFAULT_MAX_EVENT_SIZE);

    /**
     * Maximum size in bytes of a single event (JSON frame or data frame) accepted by Beats inputs.
     */
    @JsonProperty(FIELD_MAX_EVENT_SIZE)
    public abstract int maxEventSize();

    @JsonCreator
    public static BeatsInputDefaultsConfig create(@JsonProperty(FIELD_MAX_EVENT_SIZE) int maxEventSize) {
        if (maxEventSize <= 0) {
            throw new IllegalArgumentException(f("%s must be positive, but is %d", FIELD_MAX_EVENT_SIZE, maxEventSize));
        }
        return new AutoValue_BeatsInputDefaultsConfig(maxEventSize);
    }
}
