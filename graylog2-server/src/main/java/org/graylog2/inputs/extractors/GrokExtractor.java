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
import com.google.common.base.Strings;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import org.graylog2.ConfigurationException;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrokExtractor extends Extractor {
    public static final String CONFIG_GROK_PATTERN = "grok_pattern";

    private GrokPatternRegistry grokPatternRegistry;
    private String pattern;
    private boolean namedCapturesOnly;

    GrokExtractor(MetricRegistry metricRegistry,
                  GrokPatternRegistry grokPatternRegistry,
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
        super(metricRegistry,
              id,
              title,
              order,
              Type.GROK,
              cursorStrategy,
              sourceField,
              targetField,
              extractorConfig,
              creatorUserId,
              converters,
              conditionType,
              conditionValue);
        if (extractorConfig == null || Strings.isNullOrEmpty((String) extractorConfig.get(CONFIG_GROK_PATTERN))) {
            throw new ConfigurationException("grok_pattern not set");
        }

        this.grokPatternRegistry = grokPatternRegistry;
        this.pattern = (String) extractorConfig.get(CONFIG_GROK_PATTERN);
        this.namedCapturesOnly = (boolean) extractorConfig.getOrDefault("named_captures_only", false);
    }

    @Override
    protected Result[] run(String value) {
        final Grok grok = grokPatternRegistry.cachedGrokForPattern(this.pattern, this.namedCapturesOnly);

        // the extractor instance is rebuilt every second anyway
        final Match match = grok.match(value);
        final Map<String, Object> matches = match.captureFlattened();
        final List<Result> results = new ArrayList<>(matches.size());

        for (final Map.Entry<String, Object> entry : matches.entrySet()) {
            // never add null values to the results, those don't make sense for us
            if (entry.getValue() != null) {
                results.add(new Result(entry.getValue(), entry.getKey(), -1, -1));
            }
        }

        return results.toArray(new Result[0]);
    }
}
