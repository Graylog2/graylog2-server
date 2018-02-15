/**
 * This file is part of Graylog Beats Plugin.
 *
 * Graylog Beats Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Beats Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Beats Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.beats;

import com.google.inject.assistedinject.Assisted;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class BeatsTransport extends AbstractTcpTransport {
    @Inject
    public BeatsTransport(@Assisted Configuration configuration,
                          EventLoopGroup eventLoopGroup,
                          EventLoopGroupFactory eventLoopGroupFactory,
                          NettyTransportConfiguration nettyTransportConfiguration,
                          ThroughputCounter throughputCounter,
                          LocalMetricRegistry localRegistry) {
        super(configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration);
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>(super.getCustomChildChannelHandlers(input));
        handlers.put("beats", BeatsFrameDecoder::new);

        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<BeatsTransport> {
        @Override
        BeatsTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = super.getRequestedConfiguration();
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(5044);
            }
            return cr;
        }
    }
}
