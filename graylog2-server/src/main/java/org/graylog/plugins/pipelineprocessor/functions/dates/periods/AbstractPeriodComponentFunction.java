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
package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import com.google.common.primitives.Ints;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.Period;

import javax.annotation.Nonnull;

public abstract class AbstractPeriodComponentFunction extends AbstractFunction<Period> {

    private final ParameterDescriptor<Long, Period> value =
            ParameterDescriptor
                    .integer("value", Period.class)
                    .transform(this::getPeriodOfInt)
                    .build();

    private Period getPeriodOfInt(long period) {
        return getPeriod(Ints.saturatedCast(period));
    }

    @Nonnull
    protected abstract Period getPeriod(int period);

    @Override
    public Period evaluate(FunctionArgs args, EvaluationContext context) {
        return value.required(args, context);
    }

    @Override
    public FunctionDescriptor<Period> descriptor() {
        return FunctionDescriptor.<Period>builder()
                .name(getName())
                .description(getDescription())
                .pure(true)
                .returnType(Period.class)
                .params(value)
                .build();
    }

    @Nonnull
    protected abstract String getName();

    @Nonnull
    protected abstract String getDescription();
}
