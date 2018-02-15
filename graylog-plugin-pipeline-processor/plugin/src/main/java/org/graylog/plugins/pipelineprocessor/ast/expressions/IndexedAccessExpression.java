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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

public class IndexedAccessExpression extends BaseExpression {
    private final Expression indexableObject;
    private final Expression index;

    public IndexedAccessExpression(Token start, Expression indexableObject, Expression index) {
        super(start);
        this.indexableObject = indexableObject;
        this.index = index;
    }

    @Override
    public boolean isConstant() {
        return indexableObject.isConstant() && index.isConstant();
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final Object idxObj = this.index.evaluateUnsafe(context);
        final Object indexable = indexableObject.evaluateUnsafe(context);
        if (idxObj == null || indexable == null) {
            return null;
        }

        if (idxObj instanceof Long) {
            int idx = Ints.saturatedCast((long) idxObj);
            if (indexable.getClass().isArray()) {
                return Array.get(indexable, idx);
            } else if (indexable instanceof List) {
                return ((List) indexable).get(idx);
            } else if (indexable instanceof Iterable) {
                return Iterables.get((Iterable) indexable, idx);
            }
            throw new IllegalArgumentException("Object '" + indexable + "' is not an Array, List or Iterable.");
        } else if (idxObj instanceof String) {
            final String idx = idxObj.toString();
            if (indexable instanceof Map) {
                return ((Map) indexable).get(idx);
            }
            throw new IllegalArgumentException("Object '" + indexable + "' is not a Map.");
        }
        throw new IllegalArgumentException("Index '" + idxObj + "' is not a Long or String.");
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String toString() {
        return indexableObject.toString() + "[" + index.toString() + "]";
    }

    public Expression getIndexableObject() {
        return indexableObject;
    }

    public Expression getIndex() {
        return index;
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.of(indexableObject, index);
    }
}
