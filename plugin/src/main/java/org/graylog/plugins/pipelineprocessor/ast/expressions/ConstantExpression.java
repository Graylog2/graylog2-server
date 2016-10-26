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

import org.antlr.v4.runtime.Token;

import java.util.Collections;

public abstract class ConstantExpression extends BaseExpression {

    private final Class type;

    protected ConstantExpression(Token start, Class type) {
        super(start);
        this.type = type;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.emptySet();
    }
}
