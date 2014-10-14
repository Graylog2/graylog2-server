package org.graylog2.inputs.misc.jsonpath;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.JsonPathCodec;
import org.graylog2.inputs.transports.HttpPollTransport;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class JsonPathInput2 extends MessageInput2 {

    @AssistedInject
    public JsonPathInput2(@Assisted Configuration configuration,
                          @Assisted Transport transport,
                          @Assisted Codec codec,
                          MetricRegistry metricRegistry) {
        super(metricRegistry, transport, codec);
    }

    @AssistedInject
    public JsonPathInput2(@Assisted Configuration configuration,
                          HttpPollTransport.Factory transport,
                          JsonPathCodec.Factory codec,
                          MetricRegistry metricRegistry) {
        super(metricRegistry, transport.create(configuration), codec.create(configuration));
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return "JSON path from HTTP API (transport)";
    }

    @Override
    public String linkToDocs() {
        return "http://graylog2.org/resources/documentation/sending/jsonpath";
    }

    public interface Factory extends MessageInput2.Factory<JsonPathInput2> {
        @Override
        JsonPathInput2 create(Configuration configuration);

        @Override
        JsonPathInput2 create(Configuration configuration, Transport transport, Codec codec);
    }
}
