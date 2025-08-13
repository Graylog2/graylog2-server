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

import java.util.Collections;
import java.util.Map;

public class GLFortiGateSyslogEvent extends FortiGateSyslogEvent {
    protected final boolean enableKVparsing;

    public GLFortiGateSyslogEvent(String rawEvent, DateTimeZone sysLogServerTimeZone) {
        super(rawEvent, sysLogServerTimeZone);
        this.enableKVparsing = false;
    }

    public GLFortiGateSyslogEvent(String rawEvent, DateTimeZone sysLogServerTimeZone, boolean enableKVparsing) {
        super(rawEvent, sysLogServerTimeZone);
        this.enableKVparsing = enableKVparsing;
    }

    @Override
    public Map<String, String> getFields() {
        if (enableKVparsing) {
            return super.getFields();
        } else {
            return Collections.emptyMap();
        }
    }

}
