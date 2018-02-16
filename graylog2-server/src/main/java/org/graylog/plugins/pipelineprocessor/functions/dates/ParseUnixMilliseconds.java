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
package org.graylog.plugins.pipelineprocessor.functions.dates;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ParseUnixMilliseconds extends TimezoneAwareFunction {
    public static final String NAME = "parse_unix_milliseconds";

    private static final String VALUE = "value";

    private final ParameterDescriptor<Long, Long> valueParam;

    public ParseUnixMilliseconds() {
        valueParam = ParameterDescriptor.integer(VALUE).description("UNIX millisecond timestamp to parse").build();
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(valueParam);
    }

    @Override
    public DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final Long unixMillis = valueParam.required(args, context);
        return unixMillis == null ? null : new DateTime(unixMillis, timezone);
    }

    @Override
    protected String description() {
        return "Converts a UNIX millisecond timestamp into a date";
    }
}
