package org.graylog.plugins.netflow.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.netflow.codecs.NetflowCodec;
import org.graylog2.inputs.transports.UdpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class NetflowInput extends MessageInput {
    private static final String NAME = "Netflow UDP";

    @Inject
    public NetflowInput(MetricRegistry metricRegistry,
                        @Assisted Configuration configuration,
                        UdpTransport.Factory transportFactory,
                        NetflowCodec.Factory codecFactory,
                        LocalMetricRegistry localMetricRegistry,
                        Config config,
                        Descriptor descriptor,
                        ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration), localMetricRegistry,
                codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<NetflowInput> {
        @Override
        NetflowInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(UdpTransport.Factory transport, NetflowCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
