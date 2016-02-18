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
package org.graylog.plugins.pipelineprocessor.functions.ips;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.jboss.netty.handler.ipfilter.CIDR;

import java.net.UnknownHostException;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.of;

public class CidrMatch extends AbstractFunction<Boolean> {

    public static final String NAME = "cidr_match";
    public static final String CIDR_PARAM = "cidr";
    public static final StringToCIDR STRING_TO_CIDR = new StringToCIDR();
    public static final String IP = "ip";

    @Override
    public Object preComputeConstantArgument(FunctionArgs args, String name, Expression arg) {
        final Object argument = super.preComputeConstantArgument(args, name, arg);
        if (CIDR_PARAM.equals(name)) {
            //noinspection unchecked
            final Function<String, CIDR> name1 = (Function<String, CIDR>) args.param(CIDR_PARAM).transform();
            return name1.apply(argument.toString());
        }
        return argument;
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final CIDR cidr = args.param(CIDR_PARAM).evalRequired(args, context, CIDR.class);
        final IpAddress ipAddress = args.param(IP).evalRequired(args, context, IpAddress.class);
        return cidr.contains(ipAddress.inetAddress());
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        ParameterDescriptor.string(CIDR_PARAM, CIDR.class).transform(STRING_TO_CIDR).build(),
                        ParameterDescriptor.type(IP, IpAddress.class).build()
                ))
                .build();
    }

    private static class StringToCIDR implements Function<String, CIDR> {
        @Override
        public CIDR apply(String s) {
            try {
                return CIDR.newCIDR(s);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
