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
package org.graylog.integrations.inputs.paloalto;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class PaloAltoMessageBase {

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoParser.class);

    public abstract String source();
    public abstract DateTime timestamp();
    public abstract String payload();
    public abstract String panType();
    public abstract ImmutableList<String> fields();

    public static PaloAltoMessageBase create(String source, DateTime timestamp, String payload, String panType, ImmutableList<String> fields) {

        LOG.trace("Syslog header parsed successfully: " +
                  "Source {} Timestamp {} Pan Type {} Payload {}", source, timestamp, panType, payload );

        return builder()
                .source(source)
                .timestamp(timestamp)
                .payload(payload)
                .panType(panType)
                .fields(fields)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_PaloAltoMessageBase.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder source(String source);

        public abstract Builder timestamp(DateTime timestamp);

        public abstract Builder payload(String payload);

        public abstract Builder panType(String panType);

        public abstract Builder fields(ImmutableList<String> fields);

        public abstract PaloAltoMessageBase build();
    }
}
