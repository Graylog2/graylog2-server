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
package org.graylog2.inputs.codecs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.isNullOrEmpty;

public class RawCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(RawCodec.class);

    private final ObjectMapper objectMapper;
    private final Configuration configuration;

    private final Timer resolveTime;
    private final Timer decodeTime;

    @AssistedInject
    public RawCodec(ObjectMapper objectMapper, MetricRegistry metricRegistry, @Assisted Configuration configuration) {
        this.objectMapper = objectMapper;
        this.configuration = configuration;
        this.resolveTime = metricRegistry.timer(name(RawCodec.class, "resolveTime"));
        this.decodeTime = metricRegistry.timer(name(RawCodec.class, "decodeTime"));
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage raw) {
        try (Timer.Context decode = this.decodeTime.time()) {
            // TODO fix remote address, use raw message
            String source;
            if (configuration.stringIsSet(MessageInput.CK_OVERRIDE_SOURCE)) {
                source = configuration.getString(MessageInput.CK_OVERRIDE_SOURCE);
            } else {
                /*if (!isNullOrEmpty(raw.getMetaData())) {
                    try {
                        final Map<String, Object> map = objectMapper.readValue(raw.getMetaData(),
                                new TypeReference<Map<String, Object>>() {
                                });
                        source = map.containsKey("remote_address") ? (String) map.get("remote_address") : "unknown";
                    } catch (IOException e) {
                        source = "unknown";
                    }
                } else {
                    source = "unknown";
                }*/
                final InetSocketAddress remoteAddress = raw.getRemoteAddress();
                try (Timer.Context context = this.resolveTime.time()) {
                    source = Tools.rdnsLookup(remoteAddress.getAddress());
                } catch (UnknownHostException e) {
                    LOG.warn("Reverse DNS lookup failed. Falling back to parsed hostname.", e);
                    source = "unknown";
                }

            }
            return new Message(new String(raw.getPayload(), Charsets.UTF_8), source, raw.getTimestamp());
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return "raw";
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<RawCodec> {
        @Override
        RawCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(5555);
            }
        }
    }

}
