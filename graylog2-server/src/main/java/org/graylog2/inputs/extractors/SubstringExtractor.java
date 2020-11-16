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
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SubstringExtractor extends Extractor {

    private int beginIndex = -1;
    private int endIndex = -1;

    public SubstringExtractor(MetricRegistry metricRegistry,
                              String id,
                              String title,
                              long order,
                              CursorStrategy cursorStrategy,
                              String sourceField,
                              String targetField,
                              Map<String, Object> extractorConfig,
                              String creatorUserId,
                              List<Converter> converters,
                              ConditionType conditionType,
                              String conditionValue) throws ReservedFieldException, ConfigurationException {
        super(metricRegistry, id, title, order, Type.SUBSTRING, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);

        if (extractorConfig == null || extractorConfig.get("begin_index") == null || extractorConfig.get("end_index") == null) {
            throw new ConfigurationException("Missing configuration fields. Required: begin_index, end_index");
        }

        try {
            beginIndex = (Integer) extractorConfig.get("begin_index");
            endIndex = (Integer) extractorConfig.get("end_index");
        } catch (ClassCastException e) {
            throw new ConfigurationException("Index positions cannot be casted to Integer.");
        }
    }

    @Override
    protected Result[] run(String value) {
        return new Result[]{new Result(Tools.safeSubstring(value, beginIndex, endIndex), beginIndex, endIndex)};
    }

}
