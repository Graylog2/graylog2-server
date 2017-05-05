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
    private static final String CONFIG_LUT_NAME = "lookup_table_name";
    private final String sourceField;
    private final String targetField;
    private final LookupTableService.Function lookupTable;

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

        this.sourceField = sourceField;
        this.targetField = targetField;

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

        final Object value = result.getSingleValue();
        if (value == null) {
            return null;
        }

        return new Result[]{ new Result(value, targetField, -1, -1) };
    }
}
