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
package org.graylog2.inputs.transports;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.syslog.tcp.SyslogOctetCountFrameDecoder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.collections.Pair;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.TransportFactory;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.channel.ChannelHandler;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.Executor;

public class SyslogTcpOctetCountTransport extends AbstractTcpTransport {
    @AssistedInject
    public SyslogTcpOctetCountTransport(@Assisted Configuration configuration,
                                        @Named("bossPool") Executor bossPool,
                                        @Named("cached") Provider<Executor> workerPoolProvider,
                                        ThroughputCounter throughputCounter,
                                        ConnectionCounter connectionCounter,
                                        LocalMetricRegistry localRegistry) {
        super(configuration, throughputCounter, localRegistry, bossPool, workerPoolProvider, connectionCounter);
    }

    @Override
    protected List<Pair<String, ? extends ChannelHandler>> getFinalChannelHandlers(MessageInput input) {
        final List<Pair<String, ? extends ChannelHandler>> finalChannelHandlers = Lists.newArrayList();

        finalChannelHandlers.add(Pair.of("framer", new SyslogOctetCountFrameDecoder()));
        finalChannelHandlers.addAll(super.getFinalChannelHandlers(input));

        return finalChannelHandlers;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest x = super.getRequestedConfiguration();

        return x;
    }

    public interface Factory extends TransportFactory<SyslogTcpOctetCountTransport> {
        SyslogTcpOctetCountTransport create(Configuration configuration);
    }
}
