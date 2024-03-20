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
package org.graylog.plugins.pipelineprocessor.functions.conversion;

import au.com.bytecode.opencsv.CSVParser;
import com.google.common.collect.Maps;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class CsvMapConversion extends AbstractFunction<Map> {
    private final Logger log = LoggerFactory.getLogger(CsvMapConversion.class);


    public static final String NAME = "csv_to_map";
    private static final String VALUE = "value";

    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> fieldsParam;
    private final ParameterDescriptor<String, Character> separatorParam;
    private final ParameterDescriptor<String, Character> quoteCharParam;
    private final ParameterDescriptor<String, Character> escapeCharParam;
    private final ParameterDescriptor<Boolean, Boolean> strictQuotesParam;
    private final ParameterDescriptor<Boolean, Boolean> trimParam;


    public CsvMapConversion() {
        this.valueParam = string(VALUE).ruleBuilderVariable().description("Map-like value to convert").build();
        this.fieldsParam = string("fieldNames").description("List of field names separated by the <separator> character").build();
        this.separatorParam = string("separator", Character.class).optional().transform(this::getFirstChar).description("Character to split lines by, will be shortened to first character (default is <,>)").build();
        this.quoteCharParam = string("quoteChar", Character.class).optional().transform(this::getFirstChar).description("Character used to quote fields (default is <\">)").build();
        this.escapeCharParam = string("escapeChar", Character.class).optional().transform(this::getFirstChar).description("Character used to escape the separator and quote characters (default is <\\>)").build();
        this.strictQuotesParam = bool("strictQuotes").optional().description("Ignore content outside of quotes").build();
        this.trimParam = bool("trimLeadingWhitespace").optional().description("Trim leading whitespace").build();
    }

    private Character getFirstChar(String s) {
        if (!isNullOrEmpty(s))
            return s.charAt(0);
        return null;
    }

    @Override
    public Map evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String fields = fieldsParam.required(args, context);
        final Character separator = separatorParam.optional(args, context).orElse(CSVParser.DEFAULT_SEPARATOR);
        final Character quoteChar = quoteCharParam.optional(args, context).orElse(CSVParser.DEFAULT_QUOTE_CHARACTER);
        final Character escapeChar = escapeCharParam.optional(args, context).orElse(CSVParser.DEFAULT_ESCAPE_CHARACTER);
        final boolean strictQuotes = strictQuotesParam.optional(args, context).orElse(false);
        final boolean trimLeadingWhiteSpace = trimParam.optional(args, context).orElse(true);

        final CSVParser parser = new CSVParser(separator,
                quoteChar,
                escapeChar,
                strictQuotes,
                trimLeadingWhiteSpace);

        try {
            String[] fieldNames = parser.parseLine(fields);
            if (fieldNames.length == 0) {
                log.error("No field names found");
                return Collections.emptyMap();
            }

            final Map<String, String> map = Maps.newHashMap();
            try {
                final String[] strings = parser.parseLine(value);
                if (strings.length != fieldNames.length) {
                    log.error("Different number of columns in CSV data ({}) and configured field names ({}). Discarding input.",
                            strings.length, fieldNames.length);
                    return Collections.emptyMap();
                }
                for (int i = 0; i < strings.length; i++) {
                    map.put(fieldNames[i], strings[i]);
                }
            } catch (IOException e) {
                log.error("Invalid CSV input, discarding input", e);
                return Collections.emptyMap();
            }
            return map;

        } catch (IOException e) {
            log.error("Error parsing csv", e.getMessage());
            return Collections.emptyMap();
        }


    }

    @Override
    public FunctionDescriptor<Map> descriptor() {
        return FunctionDescriptor.<Map>builder()
                .name(NAME)
                .returnType(Map.class)
                .params(of(valueParam,
                        fieldsParam,
                        separatorParam,
                        quoteCharParam,
                        escapeCharParam,
                        strictQuotesParam,
                        trimParam
                ))
                .description("Converts a single line of a CSV string into a map usable by set_fields()")
                .ruleBuilderEnabled()
                .ruleBuilderName("Convert CSV to map")
                .ruleBuilderTitle("Convert CSV string '${value}' to map")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.CONVERSION)
                .build();
    }
}
