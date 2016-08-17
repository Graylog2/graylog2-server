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
import java.util.IllegalFormatException;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;

public class IpAddressConversion extends AbstractFunction<IpAddress> {

    public static final String NAME = "to_ip";
    private static final InetAddress ANYV4 = InetAddresses.forString("0.0.0.0");

    private final ParameterDescriptor<Object, Object> ipParam;
    private final ParameterDescriptor<String, String> defaultParam;

    public IpAddressConversion() {
        ipParam = ParameterDescriptor.object("ip").description("Value to convert").build();
        defaultParam = ParameterDescriptor.string("default").optional().description("Used when 'ip' is null or malformed, defaults to '0.0.0.0'").build();
    }

    @Override
    public IpAddress evaluate(FunctionArgs args, EvaluationContext context) {
        final String ipString = String.valueOf(ipParam.required(args, context));

        try {
            final InetAddress inetAddress = InetAddresses.forString(ipString);
            return new IpAddress(inetAddress);
        } catch (IllegalArgumentException e) {
            final Optional<String> defaultValue = defaultParam.optional(args, context);
            if (!defaultValue.isPresent()) {
                return new IpAddress(ANYV4);
            }
            try {
                return new IpAddress(InetAddresses.forString(defaultValue.get()));
            } catch (IllegalFormatException e1) {
                log.warn("Parameter `default` for to_ip() is not a valid IP address: {}", defaultValue.get());
                throw e1;
            }
        }
    }

    @Override
    public FunctionDescriptor<IpAddress> descriptor() {
        return FunctionDescriptor.<IpAddress>builder()
                .name(NAME)
                .returnType(IpAddress.class)
                .params(of(
                        ipParam,
                        defaultParam
                ))
                .description("Converts a value to an IPAddress using its string representation")
                .build();
    }
}
