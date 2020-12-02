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
package org.graylog2.inputs.converters;

import org.graylog2.ConfigurationException;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.lookup.LookupResult;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class LookupTableConverter extends Converter {
    private static final String CONFIG_LOOKUP_TABLE_NAME = "lookup_table_name";

    private final LookupTableService.Function lookupTable;

    public LookupTableConverter(Map<String, Object> config, LookupTableService lookupTableService) throws ConfigurationException {
        super(Type.LOOKUP_TABLE, config);

        final String lookupTableName = (String) config.get(CONFIG_LOOKUP_TABLE_NAME);

        if (isNullOrEmpty(lookupTableName)) {
            throw new ConfigurationException("Missing converter config value: " + CONFIG_LOOKUP_TABLE_NAME);
        }
        if (!lookupTableService.hasTable(lookupTableName)) {
            throw new IllegalStateException("Configured lookup table <" + lookupTableName + "> doesn't exist");
        }

        this.lookupTable = lookupTableService.newBuilder().lookupTable(lookupTableName).build();
    }

    @Override
    public Object convert(String value) {
        final LookupResult result = lookupTable.lookup(value);

        if (result == null || result.isEmpty()) {
            return value;
        }
        return result.singleValue();
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
