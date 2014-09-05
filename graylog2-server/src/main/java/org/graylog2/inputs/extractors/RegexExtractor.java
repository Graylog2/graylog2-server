/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexExtractor extends Extractor {
    private static final String CONFIG_REGEX_VALUE = "regex_value".intern();

    private final Pattern pattern;

    public RegexExtractor(final MetricRegistry metricRegistry,
                          final String id,
                          final String title,
                          final int order,
                          final CursorStrategy cursorStrategy,
                          final String sourceField,
                          final String targetField,
                          final Map<String, Object> extractorConfig,
                          final String creatorUserId,
                          final List<Converter> converters,
                          final ConditionType conditionType,
                          final String conditionValue) throws ReservedFieldException, ConfigurationException {
        super(metricRegistry, id, title, order, Type.REGEX, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);

        if (extractorConfig == null || extractorConfig.get(CONFIG_REGEX_VALUE) == null || ((String) extractorConfig.get(CONFIG_REGEX_VALUE)).isEmpty()) {
            throw new ConfigurationException("Missing regex configuration field: regex_value");
        }

        pattern = Pattern.compile((String) extractorConfig.get(CONFIG_REGEX_VALUE), Pattern.DOTALL);
    }

    @Override
    protected Result run(String value) {
        final Matcher matcher = pattern.matcher(value);

        if (!matcher.find() || matcher.groupCount() == 0 || matcher.start(1) == -1 || matcher.end(1) == -1) {
            return null;
        }

        return new Result(value.substring(matcher.start(1), matcher.end(1)), matcher.start(1), matcher.end(1));
    }

}
