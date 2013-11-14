/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.inputs.extractors;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorFactory {

    // TODO: This parameter list is growing a bit out of control.
    public static Extractor factory(String id,
                                    String title,
                                    Extractor.CursorStrategy cursorStrategy,
                                    Extractor.Type type,
                                    String sourceField,
                                    String targetField,
                                    Map<String, Object> extractorConfig,
                                    String creatorUserId, List<Converter> converters,
                                    Extractor.ConditionType conditionType,
                                    String conditionValue)
            throws NoSuchExtractorException, Extractor.ReservedFieldException, ConfigurationException {

        switch (type) {
            case REGEX:
                return new RegexExtractor(id, title, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case SUBSTRING:
                return new SubstringExtractor(id, title, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case SPLIT_AND_INDEX:
                return new SplitAndIndexExtractor(id, title, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            case COPY_INPUT:
                return new CopyInputExtractor(id, title, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);
            default:
                throw new NoSuchExtractorException();
        }
    }

    public static class NoSuchExtractorException extends Exception {
    }
}
