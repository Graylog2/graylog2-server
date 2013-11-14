/*
 * Copyright 2013 TORCH UG
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
 */
package org.graylog2.inputs.extractors;

import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

public class CopyInputExtractor extends Extractor {
    public CopyInputExtractor(String id, String title, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig, String creatorUserId, List<Converter> converters, ConditionType conditionType, String conditionValue) throws ReservedFieldException {
        super(id,
              title,
              Type.COPY_INPUT,
              cursorStrategy,
              sourceField,
              targetField,
              extractorConfig,
              creatorUserId,
              converters,
              conditionType,
              conditionValue);
    }

    @Override
    protected Result run(String value) {
        return new Result(value, 0, value.length());
    }
}
