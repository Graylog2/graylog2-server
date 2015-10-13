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
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class ExtractorFactory {
    private final MetricRegistry metricRegistry;
    private final GrokPatternService grokPatternService;

    @Inject
    public ExtractorFactory(MetricRegistry metricRegistry, GrokPatternService grokPatternService) {
        this.metricRegistry = metricRegistry;
        this.grokPatternService = grokPatternService;
    }

    public Extractor factory(String id,
                             String title,
                             long order,
                             Extractor.CursorStrategy cursorStrategy,
                             Extractor.Type type,
                             String sourceField,
                             String targetField,
                             Map<String, Object> extractorConfig,
                             String creatorUserId, List<Converter> converters,
                             Extractor.ConditionType conditionType,
                             String conditionValue)
            throws NoSuchExtractorException, Extractor.ReservedFieldException, ConfigurationException {

        // TODO convert to guice factory
        switch (type) {
            case REGEX:
                return new RegexExtractor(metricRegistry, id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case SUBSTRING:
                return new SubstringExtractor(metricRegistry, id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case SPLIT_AND_INDEX:
                return new SplitAndIndexExtractor(metricRegistry, id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case COPY_INPUT:
                return new CopyInputExtractor(metricRegistry, id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case REGEX_REPLACE:
                return new RegexReplaceExtractor(metricRegistry, id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case GROK:
                return new GrokExtractor(metricRegistry, grokPatternService.loadAll(), id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case JSON:
                return new JsonExtractor(metricRegistry, id, title, order, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            default:
                throw new NoSuchExtractorException();
        }
    }

    public static class NoSuchExtractorException extends Exception {
    }
}
