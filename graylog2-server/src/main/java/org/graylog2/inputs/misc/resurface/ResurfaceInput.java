package org.graylog2.inputs.misc.resurface;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.ResurfaceCodec;
import org.graylog2.inputs.transports.ResurfaceHttpTransport;
import org.graylog2.plugin.DocsHelper;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import javax.inject.Inject;

public class ResurfaceInput extends MessageInput {

    private static final String NAME = "Resurface API Security by Graylog";
    private static final String LINK_TO_DOCS = "https://resurface.io/docs";

    @AssistedInject
    public ResurfaceInput(@Assisted Configuration configuration, ResurfaceHttpTransport.Factory transport,
                          ResurfaceCodec.Factory codec, MetricRegistry metricRegistry,
                          LocalMetricRegistry localRegistry, Config config, Descriptor descriptor,
                          ServerStatus serverStatus) {
        super(metricRegistry, configuration, transport.create(configuration), localRegistry,
                codec.create(configuration), config, descriptor, serverStatus);
    }

    public interface Factory extends MessageInput.Factory<ResurfaceInput> {
        @Override
        ResurfaceInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, LINK_TO_DOCS);
        }

        @Override
        public boolean isCloudCompatible() {
            return true;
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(ResurfaceHttpTransport.Factory transport, ResurfaceCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
