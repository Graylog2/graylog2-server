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

public class Now extends TimezoneAwareFunction {

    public static final String NAME = "now";

    @Override
    protected DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        return DateTime.now(timezone);
    }

    @Override
    protected String description() {
        return "Returns the current time";
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of();
    }
}
