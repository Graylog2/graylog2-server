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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
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
