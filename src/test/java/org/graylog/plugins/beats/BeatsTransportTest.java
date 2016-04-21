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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BeatsTransportTest {
    @Test
    public void getFinalChannelHandlers() throws Exception {
        final BeatsTransport transport = new BeatsTransport(
                new Configuration(null),
                new ThroughputCounter(new HashedWheelTimer()),
                new LocalMetricRegistry(),
                Executors.newSingleThreadExecutor(),
                new ConnectionCounter(),
                new ObjectMapper()
        );

        final MessageInput input = mock(MessageInput.class);
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> channelHandlers = transport.getFinalChannelHandlers(input);
        assertThat(channelHandlers).containsKey("beats");
    }
}