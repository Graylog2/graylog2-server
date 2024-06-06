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
package org.graylog.scheduler.schedule;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.graylog.events.rest.CronValidationRequest;
import org.graylog.events.rest.CronValidationResponse;

import java.util.Locale;

import static com.cronutils.model.CronType.QUARTZ;

public class CronUtils {

    private static final CronDescriptor DESCRIPTOR = CronDescriptor.instance(Locale.ENGLISH);
    private static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));

    public static CronDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static CronParser getParser() {
        return PARSER;
    }

    /**
     * Validates a cron expression and throws an exception if validation fails.
     *
     * @param expression cron expression to validate
     */
    public static void validateExpression(String expression) {
        final Cron cron = PARSER.parse(expression);
        cron.validate();
    }

    /**
     * Validates a cron expression and returns a response with either the error or a description of the cron.
     *
     * @param request cron expression to validate
     * @return validation response with an error message if validation fails or a description of the cron if successful
     */
    public static CronValidationResponse validateExpression(CronValidationRequest request) {
        try {
            final Cron cron = PARSER.parse(request.expression());
            return new CronValidationResponse(null, DESCRIPTOR.describe(cron));
        } catch (IllegalArgumentException e) {
            return new CronValidationResponse(e.getMessage(), null);
        }
    }
}
