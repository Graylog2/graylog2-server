package org.graylog.plugins.messageprocessor.ast.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;

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
