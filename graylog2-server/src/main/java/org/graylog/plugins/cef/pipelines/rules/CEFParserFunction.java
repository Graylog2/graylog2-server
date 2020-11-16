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
package org.graylog.plugins.cef.pipelines.rules;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.jcustenborder.cef.CEFParser;
import com.github.jcustenborder.cef.CEFParserFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.graylog.plugins.cef.parser.MappedMessage;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

public class CEFParserFunction extends AbstractFunction<CEFParserResult> {
    private static final Logger LOG = LoggerFactory.getLogger(CEFParserFunction.class);

    public static final String NAME = "parse_cef";

    @VisibleForTesting
    static final String VALUE = "cef_string";
    static final String USE_FULL_NAMES = "use_full_names";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The CEF string to parse").build();
    private final ParameterDescriptor<Boolean, Boolean> useFullNamesParam = ParameterDescriptor.bool(USE_FULL_NAMES).description("Use full field names for CEF extensions").build();

    private final Timer parseTime;

    @Inject
    public CEFParserFunction(final MetricRegistry metricRegistry) {
        this.parseTime = metricRegistry.timer(name(this.getClass(), "parseTime"));
    }

    @Override
    public CEFParserResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String cef = valueParam.required(args, context);
        final boolean useFullNames = useFullNamesParam.optional(args, context).orElse(false);

        final CEFParser parser = CEFParserFactory.create();
        if (cef == null || cef.isEmpty()) {
            LOG.debug("NULL or empty parameter passed to CEF parser function. Not evaluating.");
            return null;
        }

        LOG.debug("Running CEF parser for [{}].", cef);

        final MappedMessage message;
        try (Timer.Context timer = parseTime.time()) {
            message = new MappedMessage(parser.parse(cef.trim()), useFullNames);
        } catch (Exception e) {
            LOG.error("Error while parsing CEF message: {}", cef, e);
            return null;
        }

        final Map<String, Object> fields = new HashMap<>();

        /*
          * Add all CEF standard fields. We are prefixing with cef_ to avoid overwriting existing fields or to be
          * overwritten ourselves later in the processing. The user is encouraged to run another pipeline function
          * to clean up field names if desired.
         */
        fields.put("cef_version", message.cefVersion());
        fields.put("device_vendor", message.deviceVendor());
        fields.put("device_product", message.deviceProduct());
        fields.put("device_version", message.deviceVersion());
        fields.put("device_event_class_id", message.deviceEventClassId());
        fields.put("name", message.name());
        fields.put("severity", message.severity());

        // Add all custom CEF fields.
        fields.putAll(message.mappedExtensions());

        return new CEFParserResult(fields);
    }

    @Override
    public FunctionDescriptor<CEFParserResult> descriptor() {
        return FunctionDescriptor.<CEFParserResult>builder()
                .name(NAME)
                .description("Parse any CEF formatted string into it's fields. This is the CEF string (starting with \"CEF:\") without a syslog envelope.")
                .params(valueParam, useFullNamesParam)
                .returnType(CEFParserResult.class)
                .build();
    }
}
