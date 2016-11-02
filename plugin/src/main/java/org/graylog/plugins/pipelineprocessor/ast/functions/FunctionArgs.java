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
package org.graylog.plugins.pipelineprocessor.ast.functions;

import com.google.common.collect.Maps;

import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.VarRefExpression;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.MoreObjects.firstNonNull;

public class FunctionArgs {

    @Nonnull
    private final Map<String, Expression> args;

    private final Map<String, Object> constantValues = Maps.newHashMap();
    private final Function function;
    private final FunctionDescriptor descriptor;

    public FunctionArgs(Function func, Map<String, Expression> args) {
        function = func;
        descriptor = function.descriptor();
        this.args = firstNonNull(args, Collections.<String, Expression>emptyMap());
    }

    @Nonnull
    public Map<String, Expression> getArgs() {
        return args;
    }

    @Nonnull
    public Map<String, Expression> getConstantArgs() {
        return args.entrySet().stream()
                .filter(e -> e.getValue().isConstant())
                .filter(e -> !(e.getValue() instanceof VarRefExpression)) // do not eagerly touch variables
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isPresent(String key) {
        return args.containsKey(key);
    }

    @Nullable
    public Expression expression(String key) {
        return args.get(key);
    }

    public Object getPreComputedValue(String name) {
        return constantValues.get(name);
    }

    public void setPreComputedValue(@Nonnull String name, @Nonnull Object value) {
        Objects.requireNonNull(value);
        constantValues.put(name, value);
    }

    public Function<?> getFunction() {
        return function;
    }

    public ParameterDescriptor<?, ?> param(String name) {
        return descriptor.param(name);
    }
}
