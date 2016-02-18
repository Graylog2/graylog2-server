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
