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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.firstNonNull;

public class FunctionArgs {

    @Nonnull
    private final Map<String, Expression> args;

    public FunctionArgs(Map<String, Expression> args) {
        this.args = firstNonNull(args, Collections.emptyMap());
    }

    @Nonnull
    public Map<String, Expression> getArgs() {
        return args;
    }

    @Nonnull
    public <T> Optional<T> evaluated(String name, EvaluationContext context, Class<T> argumentType) {
        final Expression valueExpr = expression(name);
        if (valueExpr == null) {
            return Optional.empty();
        }
        final Object value = valueExpr.evaluate(context);
        return Optional.of(argumentType.cast(value));
    }

    public boolean isPresent(String key) {
        return args.containsKey(key);
    }

    @Nullable
    public Expression expression(String key) {
        return args.get(key);
    }

}
