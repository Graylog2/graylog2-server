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
package org.graylog2.inputs.codecs;

import org.graylog2.syslog4j.server.impl.event.FortiGateSyslogEvent;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GLFortiGateSyslogEvent extends FortiGateSyslogEvent {
    private static final Pattern KV_PATTERN = Pattern.compile("(\\w+)=([^\\s\"]*)");
    private static final Pattern QUOTED_KV_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"");
    public GLFortiGateSyslogEvent(String rawEvent, DateTimeZone sysLogServerTimeZone) {
        super(rawEvent, sysLogServerTimeZone);
    }

    @Override
    /*
     * Filter out invalid fields that occur when the value string contains an `=`.
     * Currently this is only known to affect url strings.
     */
    public Map<String, String> getFields() {
        Map<String, String> fields = new HashMap<>(super.getFields());
        Set<String> removalKeys = new HashSet<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = entry.getValue();

            // If the value contains an `=`, flag any corresponding field for removal.
            if (value != null && value.contains("=")) {
                Matcher matcher = KV_PATTERN.matcher(value);
                while (matcher.find()) {
                    removalKeys.add(matcher.group(1));
                }

                matcher = QUOTED_KV_PATTERN.matcher(value);
                while (matcher.find()) {
                    removalKeys.add(matcher.group(1));
                }
            }
        }
        removalKeys.forEach(fields::remove);

        return fields;
    }
}
