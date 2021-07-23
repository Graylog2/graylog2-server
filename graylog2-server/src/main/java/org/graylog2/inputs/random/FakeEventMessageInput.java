package org.graylog2.inputs.random;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.RandomEventMessageCodec;
import org.graylog2.inputs.transports.RandomEventMessageTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import javax.inject.Inject;

public class FakeEventMessageInput extends MessageInput {

    private static final String NAME = "Random Event Message generator";

    @AssistedInject
    public FakeEventMessageInput(@Assisted Configuration configuration,
                                 RandomEventMessageTransport.Factory transportFactory,
                                 RandomEventMessageCodec.Factory codecFactory,
                                 MetricRegistry metricRegistry,
                                 LocalMetricRegistry localMetricRegistry,
                                 Config config,
                                 Descriptor descriptor,
                                 ServerStatus serverStatus) {
        super(metricRegistry,
                configuration,
                transportFactory.create(configuration),
                localMetricRegistry, codecFactory.create(configuration),
                config, descriptor, serverStatus);
    }

    public interface Factory extends MessageInput.Factory<FakeEventMessageInput> {
        @Override
        FakeEventMessageInput create(Configuration configuration);

        @Override
        FakeEventMessageInput.Config getConfig();

        @Override
        FakeEventMessageInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(RandomEventMessageTransport.Factory transport, RandomEventMessageCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
