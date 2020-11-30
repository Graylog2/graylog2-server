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
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;
import org.joda.time.DateTimeZone;

import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.emptyToNull;

public abstract class AbstractDateConverter extends Converter {
    private static final DateTimeZone DEFAULT_TIME_ZONE = DateTimeZone.forID("Etc/UTC");

    protected final DateTimeZone timeZone;

    public AbstractDateConverter(Type type, Map<String, Object> config) {
        super(type, config);

        this.timeZone = buildTimeZone(config.get("time_zone"));
    }

    private static DateTimeZone buildTimeZone(Object timeZoneId) {
        if (timeZoneId instanceof String) {
            try {
                final String timeZoneString = (String) timeZoneId;
                final String zoneId = firstNonNull(emptyToNull(timeZoneString.trim()), "Etc/UTC");
                return DateTimeZone.forID(zoneId);
            } catch (IllegalArgumentException e) {
                return DEFAULT_TIME_ZONE;
            }
        } else {
            return DEFAULT_TIME_ZONE;
        }
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
