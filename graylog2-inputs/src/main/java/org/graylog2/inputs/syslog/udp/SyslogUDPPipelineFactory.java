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
package org.graylog2.inputs.syslog.udp;


import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.inputs.util.PacketInformationDumper;
import org.graylog2.inputs.syslog.SyslogDispatcher;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogUDPPipelineFactory implements ChannelPipelineFactory {

    private final MetricRegistry metricRegistry;
    private final Buffer processBuffer;
    private final Configuration config;
    private final MessageInput sourceInput;
    private final ThroughputCounter throughputCounter;

    public SyslogUDPPipelineFactory(MetricRegistry metricRegistry,
                                    Buffer processBuffer,
                                    Configuration config,
                                    MessageInput sourceInput, ThroughputCounter throughputCounter) {
        this.metricRegistry = metricRegistry;
        this.processBuffer = processBuffer;
        this.config = config;
        this.sourceInput = sourceInput;
        this.throughputCounter = throughputCounter;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = Channels.pipeline();

        p.addLast("packet-meta-dumper", new PacketInformationDumper(sourceInput));
        p.addLast("traffic-counter", throughputCounter);
        p.addLast("handler", new SyslogDispatcher(metricRegistry, processBuffer, config, sourceInput));
        
        return p;
    }

}
