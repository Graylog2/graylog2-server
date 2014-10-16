/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.misc.jsonpath;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.JsonPathCodec;
import org.graylog2.inputs.transports.HttpPollTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class JsonPathInput extends MessageInput {

    @AssistedInject
    public JsonPathInput(@Assisted Configuration configuration,
                         @Assisted Transport transport,
                         @Assisted Codec codec,
                         MetricRegistry metricRegistry,
                         LocalMetricRegistry localRegistry) {
        super(metricRegistry, transport, localRegistry, codec);
    }

    @AssistedInject
    public JsonPathInput(@Assisted Configuration configuration,
                         HttpPollTransport.Factory transport,
                         JsonPathCodec.Factory codec,
                         MetricRegistry metricRegistry,
                         LocalMetricRegistry localRegistry) {
        super(metricRegistry, transport.create(configuration), localRegistry, codec.create(configuration));
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return "JSON path from HTTP API";
    }

    @Override
    public String linkToDocs() {
        return "http://graylog2.org/resources/documentation/sending/jsonpath";
    }

    public interface Factory extends MessageInput.Factory<JsonPathInput> {
        @Override
        JsonPathInput create(Configuration configuration);

        @Override
        JsonPathInput create(Configuration configuration, Transport transport, Codec codec);
    }
}
