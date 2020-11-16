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
package org.graylog.integrations.ipfix;

import com.google.auto.value.AutoValue;

import java.time.ZonedDateTime;

@AutoValue
public abstract class MessageHeader {
    /**
     * Known size of an IPFIX message header
     */
    public static final int LENGTH = 16;

    public abstract int length();

    public abstract ZonedDateTime exportTime();

    public abstract long sequenceNumber();

    public abstract long observationDomainId();

    public static MessageHeader create(int length, ZonedDateTime exportTime, long sequenceNumber, long observationDomainId) {
        return new AutoValue_MessageHeader(length, exportTime, sequenceNumber, observationDomainId);
    }
}
