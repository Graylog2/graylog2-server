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
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.jboss.netty.handler.ipfilter.CIDR;

import java.net.UnknownHostException;

import static com.google.common.collect.ImmutableList.of;

public class CidrMatch extends AbstractFunction<Boolean> {

    public static final String NAME = "cidr_match";
    public static final String IP = "ip";

    private final ParameterDescriptor<String, CIDR> cidrParam;
    private final ParameterDescriptor<IpAddress, IpAddress> ipParam;

    public CidrMatch() {
        // a little ugly because newCIDR throws a checked exception :(
        cidrParam = ParameterDescriptor.string("cidr", CIDR.class).transform(cidrString -> {
            try {
                return CIDR.newCIDR(cidrString);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        }).description("The CIDR subnet mask").build();
        ipParam = ParameterDescriptor.type(IP, IpAddress.class).description("The parsed IP address to match against the CIDR mask").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final CIDR cidr = cidrParam.required(args, context);
        final IpAddress ipAddress = ipParam.required(args, context);
        if (cidr == null || ipAddress == null) {
            return null;
        }
        return cidr.contains(ipAddress.inetAddress());
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        cidrParam,
                        ipParam))
                .description("Checks if an IP address matches a CIDR subnet mask")
                .build();
    }
}
