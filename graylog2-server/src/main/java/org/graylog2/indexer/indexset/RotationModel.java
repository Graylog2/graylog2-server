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
package org.graylog2.indexer.indexset;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.graylog2.rest.resources.entities.FilterOption;

import java.util.Arrays;
import java.util.Locale;

import static org.graylog2.indexer.indexset.fields.RotationAndRetentionFields.FIELD_DATA_TIERING;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * How an index set rotates and retains its indices: through a data tiering configuration, or through the
 * legacy rotation and retention strategies. Used by the entity table's filter.
 */
public enum RotationModel {
    DATA_TIERING("data_tiering", "Data Tiering"),
    LEGACY("legacy", "Legacy (Deprecated)");

    private final String value;
    private final String title;

    RotationModel(String value, String title) {
        this.value = value;
        this.title = title;
    }

    public String value() {
        return value;
    }

    public FilterOption filterOption() {
        return FilterOption.create(value, title);
    }

    /** Selects exactly the index sets using this rotation model. A missing or null data tiering config means legacy. */
    public Bson toBson() {
        return switch (this) {
            case DATA_TIERING -> Filters.ne(FIELD_DATA_TIERING, null);
            case LEGACY -> Filters.eq(FIELD_DATA_TIERING, null);
        };
    }

    /** @throws IllegalArgumentException for a value that names no rotation model. The filter parser maps it to a 400 response. */
    public static RotationModel fromValue(String value) {
        final String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(model -> model.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(f("Unknown rotation model: %s", value)));
    }
}
