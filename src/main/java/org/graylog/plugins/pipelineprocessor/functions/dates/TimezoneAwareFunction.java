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
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public abstract class TimezoneAwareFunction extends AbstractFunction<DateTime> {

    public static final String TIMEZONE = "timezone";

    @Override
    public Object preComputeConstantArgument(FunctionArgs args, String name, Expression arg) {
        if (name.equals(TIMEZONE)) {
            final String tz = (String) super.preComputeConstantArgument(args, name, arg);
            return DateTimeZone.forID(tz);
        }
        return super.preComputeConstantArgument(args, name, arg);
    }

    @Override
    public DateTime evaluate(FunctionArgs args, EvaluationContext context) {
        final DateTimeZone timezone =
                args.param(TIMEZONE)
                        .eval(args, context, DateTimeZone.class)
                        .orElse(DateTimeZone.UTC);

        return evaluate(args, context, timezone);
    }

    protected abstract DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone);

    @Override
    public FunctionDescriptor<DateTime> descriptor() {
        return FunctionDescriptor.<DateTime>builder()
                .name(getName())
                .returnType(DateTime.class)
                .params(ImmutableList.<ParameterDescriptor>builder()
                                .addAll(params())
                                .add(ParameterDescriptor
                                             .string(TIMEZONE, DateTimeZone.class)
                                             .optional()
                                             .build())
                                .build())
                .build();
    }

    protected abstract String getName();

    protected abstract ImmutableList<ParameterDescriptor> params();
}
