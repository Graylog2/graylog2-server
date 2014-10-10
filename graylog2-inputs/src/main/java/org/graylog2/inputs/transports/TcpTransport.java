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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.collections.Pair;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.transports.TransportFactory;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.Executor;

import static org.jboss.netty.handler.codec.frame.Delimiters.lineDelimiter;
import static org.jboss.netty.handler.codec.frame.Delimiters.nulDelimiter;

public class TcpTransport extends AbstractTcpTransport {

    public static final String CK_USE_NULL_DELIMITER = "use_null_delimiter";
    public static final String CK_MAX_MESSAGE_SIZE = "max_message_size";
    protected final ChannelBuffer[] delimiter;
    protected final int maxFrameLength;

    @AssistedInject
    public TcpTransport(@Assisted Configuration configuration,
                        @Named("bossPool") Executor bossPool,
                        @Named("cached") Provider<Executor> workerPoolProvider,
                        ThroughputCounter throughputCounter,
                        ConnectionCounter connectionCounter,
                        MetricRegistry metricRegistry,
                        ObjectMapper mapper) {
        super(configuration, throughputCounter, metricRegistry, mapper, bossPool, workerPoolProvider, connectionCounter);

        final boolean nulDelimiter = configuration.getBoolean(CK_USE_NULL_DELIMITER);
        this.delimiter = nulDelimiter ? nulDelimiter() : lineDelimiter();

        if (configuration.intIsSet(CK_MAX_MESSAGE_SIZE)) {
            maxFrameLength = (int) configuration.getInt(CK_MAX_MESSAGE_SIZE);
        } else {
            maxFrameLength = 2 * 1024 * 1024;
        }
    }

    @Override
    protected List<Pair<String, ? extends ChannelHandler>> getFinalChannelHandlers(MessageInput2 input) {
        final List<Pair<String, ? extends ChannelHandler>> finalChannelHandlers = Lists.newArrayList();

        finalChannelHandlers.add(Pair.of("framer", new DelimiterBasedFrameDecoder(maxFrameLength, delimiter)));
        finalChannelHandlers.addAll(super.getFinalChannelHandlers(input));

        return finalChannelHandlers;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest x = super.getRequestedConfiguration();

        x.addField(
                new BooleanField(
                        CK_USE_NULL_DELIMITER,
                        "Null frame delimiter?",
                        false,
                        "Use null byte as frame delimiter? Default is newline."
                )
        );
        x.addField(
                new NumberField(
                        CK_MAX_MESSAGE_SIZE,
                        "Maximum message size",
                        maxFrameLength,
                        "The maximum length of a message.",
                        ConfigurationField.Optional.OPTIONAL,
                        NumberField.Attribute.ONLY_POSITIVE
                )
        );

        return x;
    }

    public interface Factory extends TransportFactory<TcpTransport> {
        TcpTransport create(Configuration configuration);
    }
}
