/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

public class CopyInputExtractor extends Extractor {
    public CopyInputExtractor(MetricRegistry metricRegistry, String id, String title,
                              long order, CursorStrategy cursorStrategy, String sourceField,
                              String targetField, Map<String, Object> extractorConfig,
                              String creatorUserId, List<Converter> converters,
                              ConditionType conditionType, String conditionValue,
                              int status) throws ReservedFieldException {
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
            conditionValue,
            status);
    }

    @Override
    protected Result[] run(String value) {
        return new Result[]{new Result(value, 0, value.length())};
    }
}
