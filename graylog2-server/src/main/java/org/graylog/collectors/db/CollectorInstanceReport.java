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
package org.graylog.collectors.db;

import com.google.auto.value.AutoBuilder;
import org.apache.commons.collections.KeyValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public record CollectorInstanceReport(
        String instanceUid,
        long messageSeqNum,
        long capabilities,
        Instant lastSeen,
        Optional<List<Attribute>> identifyingAttributes,
        Optional<List<Attribute>> nonIdentifyingAttributes
) {
    @AutoBuilder
    public interface Builder {
        Builder instanceUid(String instanceUid);
        Builder messageSeqNum(long messageSeqNum);
        Builder capabilities(long capabilities);
        Builder lastSeen(Instant lastSeen);
        Builder identifyingAttributes(List<Attribute> identifyingAttributes);
        Builder nonIdentifyingAttributes(List<Attribute> nonIdentifyingAttributes);
        CollectorInstanceReport build();

    }

    public static Builder builder() {
        return new AutoBuilder_CollectorInstanceReport_Builder().lastSeen(Instant.now());
    }
}
