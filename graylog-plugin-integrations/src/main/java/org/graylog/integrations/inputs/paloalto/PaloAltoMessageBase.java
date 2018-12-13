/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
