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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

public class CopyInputExtractor extends Extractor {
    public CopyInputExtractor(MetricRegistry metricRegistry, String id, String title, long order, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig, String creatorUserId, List<Converter> converters, ConditionType conditionType, String conditionValue) throws ReservedFieldException {
        super(metricRegistry,
              id,
              title,
              order,
              Type.COPY_INPUT,
              cursorStrategy,
              sourceField,
              targetField,
              extractorConfig,
              creatorUserId,
              converters,
              conditionType,
              conditionValue);
    }

    @Override
    protected Result[] run(String value) {
        return new Result[] { new Result(value, 0, value.length())};
    }
}
