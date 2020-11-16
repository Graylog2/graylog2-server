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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.emptyToNull;

public class SplitAndIndexExtractor extends Extractor {

    public final String splitChar;
    public final int index;

    public SplitAndIndexExtractor(MetricRegistry metricRegistry,
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
        super(metricRegistry, id, title, order, Type.SPLIT_AND_INDEX, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue);

        if (extractorConfig == null || extractorConfig.get("index") == null || extractorConfig.get("split_by") == null) {
            throw new ConfigurationException("Missing configuration fields. Required: index, split_by");
        }

        try {
            index = ((Integer) extractorConfig.get("index")) - 1;
            splitChar = (String) extractorConfig.get("split_by");
        } catch (ClassCastException e) {
            throw new ConfigurationException("Parameters cannot be casted.");
        }
    }

    @Override
    protected Result[] run(String value) {
        String result = cut(value, splitChar, index);

        if (result == null) {
            return null;
        }

        int[] range = getCutIndices(value, splitChar, index);

        return new Result[]{new Result(result, range[0], range[1])};
    }

    public static String cut(String s, String splitChar, int index) {
        if (s == null || splitChar == null || index < 0) {
            return null;
        }

        final String[] parts = s.split(Pattern.quote(splitChar));
        if (parts.length <= index) {
            return null;
        }

        return emptyToNull(parts[index]);
    }

    public static int[] getCutIndices(String s, String splitChar, int index) {
        int found = 0;
        char target = splitChar.charAt(0);

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == target) {
                found++;
            }

            if (found == index) {
                int begin = i;
                if (begin != 0) {
                    begin += 1;
                }

                int end = s.indexOf(target, i + 1);

                // End will be -1 if this is the last last token in the string and there is no other occurence.
                if (end == -1) {
                    end = s.length();
                }

                return new int[]{begin, end};
            }
        }

        return new int[]{0, 0};
    }

}
