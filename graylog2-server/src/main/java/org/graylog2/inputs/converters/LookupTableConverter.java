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
        return result.getSingleValue();
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
