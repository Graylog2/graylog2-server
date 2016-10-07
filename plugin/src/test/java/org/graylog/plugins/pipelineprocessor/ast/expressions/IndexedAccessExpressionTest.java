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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class IndexedAccessExpressionTest {

    public static final Token START = new CommonToken(-1);
    private EvaluationContext context;

    @Before
    public void setup() {
        context = new EvaluationContext(new Message("test message", "test", DateTime.parse("2010-07-30T16:03:25Z")));
    }

    @Test
    public void accessArray() {
        int ary[] = new int[] {23};
        final IndexedAccessExpression idxExpr = new IndexedAccessExpression(START, obj(ary), num(0));

        final Object evaluate = idxExpr.evaluateUnsafe(context);
        assertThat(evaluate).isOfAnyClassIn(Integer.class);
        assertThat(evaluate).isEqualTo(23);
    }

    @Test
    public void accessList() {
        final ImmutableList<Integer> list = ImmutableList.of(23);
        final IndexedAccessExpression idxExpr = new IndexedAccessExpression(START, obj(list), num(0));

        final Object evaluate = idxExpr.evaluateUnsafe(context);
        assertThat(evaluate).isOfAnyClassIn(Integer.class);
        assertThat(evaluate).isEqualTo(23);
    }

    @Test
    public void accessIterable() {
        final Iterable<Integer> iterable = () -> new AbstractIterator<Integer>() {
            private boolean done = false;

            @Override
            protected Integer computeNext() {
                if (done) {
                    return endOfData();
                }
                done = true;
                return 23;
            }
        };
        final IndexedAccessExpression idxExpr = new IndexedAccessExpression(START, obj(iterable), num(0));

        final Object evaluate = idxExpr.evaluateUnsafe(context);
        assertThat(evaluate).isOfAnyClassIn(Integer.class);
        assertThat(evaluate).isEqualTo(23);
    }

    @Test
    public void accessMap() {
        final ImmutableMap<String, Integer> map = ImmutableMap.of("string", 23);
        final IndexedAccessExpression idxExpr = new IndexedAccessExpression(START, obj(map), string("string"));

        final Object evaluate = idxExpr.evaluateUnsafe(context);
        assertThat(evaluate).isEqualTo(23);
    }

    @Test
    public void invalidObject() {
        final IndexedAccessExpression expression = new IndexedAccessExpression(START, obj(23), num(0));

        // this should throw an exception
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> expression.evaluateUnsafe(context));
    }

    private static Expression num(long idx) {
        return new LongExpression(START, idx);
    }

    private static Expression string(String string) {
        return new StringExpression(START, string);
    }

    private static ConstantObjectExpression obj(Object object) {
        return new ConstantObjectExpression(object);
    }

    private static class ConstantObjectExpression extends ConstantExpression {
        private final Object object;

        protected ConstantObjectExpression(Object object) {
            super(START, object.getClass());
            this.object = object;
        }

        @Override
        public Object evaluateUnsafe(EvaluationContext context) {
            return object;
        }
    }
}