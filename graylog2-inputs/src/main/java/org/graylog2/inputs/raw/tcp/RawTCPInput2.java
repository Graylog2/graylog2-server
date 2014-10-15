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
/**
 *
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
package org.graylog2.inputs.raw.tcp;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.RawCodec;
import org.graylog2.inputs.transports.TcpTransport;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class RawTCPInput2 extends MessageInput2 {

    @AssistedInject
    public RawTCPInput2(@Assisted final Configuration configuration,
                        @Assisted final Transport transport,
                        @Assisted final Codec codec,
                        final MetricRegistry metricRegistry) {
        super(metricRegistry, transport, codec);
    }

    @AssistedInject
    public RawTCPInput2(@Assisted final Configuration configuration,
                        final TcpTransport.Factory tcpTransportFactory,
                        final RawCodec.Factory rawCodecFactory,
                        final MetricRegistry metricRegistry) {
        super(metricRegistry, tcpTransportFactory.create(configuration), rawCodecFactory.create(configuration));
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return "Raw/Plaintext TCP (transport based)";
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    public interface Factory extends MessageInput2.Factory<RawTCPInput2> {
        RawTCPInput2 create(Configuration configuration);
        RawTCPInput2 create(Configuration configuration, Transport transport, Codec codec);
    }
}
