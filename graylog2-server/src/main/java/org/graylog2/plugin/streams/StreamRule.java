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
package org.graylog2.plugin.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.graylog2.plugin.database.Persisted;

import java.util.Map;

@JsonAutoDetect
public interface StreamRule extends Persisted {
    @Override
    String getId();

    StreamRuleType getType();

    String getField();

    String getValue();

    Boolean getInverted();

    String getStreamId();

    String getContentPack();

    String getDescription();

    void setType(StreamRuleType type);

    void setField(String field);

    void setValue(String value);

    void setInverted(Boolean inverted);

    void setContentPack(String contentPack);

    void setDescription(String description);

    @Override
    Map<String, Object> asMap();
}
