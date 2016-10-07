/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.dates;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ParseDate extends TimezoneAwareFunction {

    public static final String NAME = "parse_date";
    public static final String VALUE = "value";
    public static final String PATTERN = "pattern";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> patternParam;

    public ParseDate() {
        valueParam = ParameterDescriptor.string(VALUE).description("Date string to parse").build();
        patternParam = ParameterDescriptor.string(PATTERN).description("The pattern to parse the date with, see http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html").build();
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(
                valueParam,
                patternParam
        );
    }

    @Override
    public DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final String dateString = valueParam.required(args, context);
        final String pattern = patternParam.required(args, context);
        if (dateString == null || pattern == null) {
            return null;
        }
        final DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern).withZone(timezone);

        return formatter.parseDateTime(dateString);
    }

    @Override
    protected String description() {
        return "Parses a date string using the given date format";
    }
}
