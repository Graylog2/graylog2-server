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
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class MapLiteralExpression extends BaseExpression {
    private final HashMap<String, Expression> map;

    public MapLiteralExpression(Token start, HashMap<String, Expression> map) {
        super(start);
        this.map = map;
    }

    @Override
    public boolean isConstant() {
        return map.values().stream().allMatch(Expression::isConstant);
    }

    @Override
    public Map evaluateUnsafe(EvaluationContext context) {
        // evaluate all values for each key and return the resulting map
        return Seq.seq(map)
                .map(entry -> entry.map2(value -> value.evaluateUnsafe(context)))
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

    public Iterable<Map.Entry<String, Expression>> entries() {
        return ImmutableSet.copyOf(map.entrySet());
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.copyOf(map.values());
    }
}
