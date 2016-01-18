package org.graylog.plugins.messageprocessor.ast.expressions;

import com.google.common.base.Joiner;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class MapExpression implements Expression {
    private final HashMap<String, Expression> map;

    public MapExpression(HashMap<String, Expression> map) {
        this.map = map;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        // evaluate all values for each key and return the resulting map
        return Seq.seq(map)
                .map(entry -> entry.map2(value -> value.evaluate(context)))
                .toMap(Tuple2::v1, Tuple2::v2);
    }

    @Override
    public Class getType() {
        return Map.class;
    }

    @Override
    public String toString() {
        return "{" + Joiner.on(", ").withKeyValueSeparator(":").join(map) + "}";
    }
}
