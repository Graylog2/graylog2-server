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
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.lookup.LookupResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class LookupTableExtractor extends Extractor {
    private final LookupTableService.Function lookupTable;
    public static final String CONFIG_LUT_NAME = "lookup_table_name";

    public LookupTableExtractor(final MetricRegistry metricRegistry,
                                final LookupTableService lookupTableService,
                                final String id,
                                final String title,
                                final long order,
                                final CursorStrategy cursorStrategy,
                                final String sourceField,
                                final String targetField,
                                final Map<String, Object> extractorConfig,
                                final String creatorUserId,
                                final List<Converter> converters,
                                final ConditionType conditionType,
                                final String conditionValue) throws ReservedFieldException, ConfigurationException {
        super(metricRegistry, id, title, order, Type.LOOKUP_TABLE, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);

        final String lookupTableName = (String) extractorConfig.get(CONFIG_LUT_NAME);
        if (isNullOrEmpty(lookupTableName)) {
            throw new ConfigurationException("Missing lookup table extractor configuration field: " + CONFIG_LUT_NAME);
        }

        if (!lookupTableService.hasTable(lookupTableName)) {
            throw new IllegalStateException("Configured lookup table <" + lookupTableName + "> doesn't exist");
        }

        this.lookupTable = lookupTableService.newBuilder().lookupTable(lookupTableName).build();
    }

    @Override
    @Nullable
    protected Result[] run(String sourceFieldValue) {
        final LookupResult result = lookupTable.lookup(sourceFieldValue);

        if (result == null || result.isEmpty()) {
            return null;
        }

        final Object value = result.singleValue();
        if (value == null) {
            return null;
        }

        return new Result[]{ new Result(value, targetField, -1, -1) };
    }
}
