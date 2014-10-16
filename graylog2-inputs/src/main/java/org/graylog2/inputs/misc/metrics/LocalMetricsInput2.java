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
package org.graylog2.inputs.misc.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.GelfCodec;
import org.graylog2.inputs.transports.LocalMetricsTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class LocalMetricsInput2 extends MessageInput2 {

    @AssistedInject
    public LocalMetricsInput2(@Assisted Configuration configuration,
                              MetricRegistry metricRegistry,
                              LocalMetricsTransport.Factory transport,
                              GelfCodec.Factory codec, LocalMetricRegistry localRegistry) {
        super(metricRegistry, transport.create(configuration), codec.create(configuration), localRegistry);
    }

    @AssistedInject
    public LocalMetricsInput2(@Assisted Configuration configuration,
                              MetricRegistry metricRegistry,
                              @Assisted Transport transport,
                              @Assisted Codec codec, LocalMetricRegistry localRegistry) {
        super(metricRegistry, transport, codec, localRegistry);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public String getName() {
        return "Internal metrics reporter (transport)";
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    public interface Factory extends MessageInput2.Factory<LocalMetricsInput2> {
        @Override
        LocalMetricsInput2 create(Configuration configuration);

        @Override
        LocalMetricsInput2 create(Configuration configuration, Transport transport, Codec codec);
    }
}
