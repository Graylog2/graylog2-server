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
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Optional;

public class FlexParseDate extends TimezoneAwareFunction {

    public static final String VALUE = "value";
    public static final String NAME = "flex_parse_date";
    public static final String DEFAULT = "default";

    @Override
    protected DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final String time = args.param(VALUE).evalRequired(args, context, String.class);
        final DateTimeZone timeZone = args.param(TIMEZONE).evalRequired(args, context, DateTimeZone.class);

        final List<DateGroup> dates = new Parser(timeZone.toTimeZone()).parse(time);
        if (dates.size() == 0) {
            final Optional<DateTime> defaultTime = args.param(DEFAULT).eval(args, context, DateTime.class);
            if (defaultTime.isPresent()) {
                return defaultTime.get();
            }
            // TODO really? this should probably throw an exception of some sort to be handled in the interpreter
            return null;
        }
        return new DateTime(dates.get(0).getDates().get(0), timeZone);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(
                ParameterDescriptor.string(VALUE).build(),
                ParameterDescriptor.type(DEFAULT, DateTime.class).optional().build()
        );
    }
}
