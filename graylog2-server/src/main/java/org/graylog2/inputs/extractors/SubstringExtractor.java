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
                              String conditionValue,
                              int status) throws ReservedFieldException, ConfigurationException {
        super(metricRegistry, id, title, order, Type.SUBSTRING, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue, status);

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
