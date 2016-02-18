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

import com.google.common.net.InetAddresses;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.net.InetAddress;

import static com.google.common.collect.ImmutableList.of;

public class IpAddressConversion extends AbstractFunction<IpAddress> {

    public static final String IP = "ip";
    public static final String NAME = "toip";

    @Override
    public IpAddress evaluate(FunctionArgs args, EvaluationContext context) {
        final String ipString = args.param(IP).evalRequired(args, context, String.class);

        final InetAddress inetAddress = InetAddresses.forString(ipString);
        return new IpAddress(inetAddress);
    }

    @Override
    public FunctionDescriptor<IpAddress> descriptor() {
        return FunctionDescriptor.<IpAddress>builder()
                .name(NAME)
                .returnType(IpAddress.class)
                .params(of(
                        ParameterDescriptor.string(IP).build()
                ))
                .build();
    }
}
