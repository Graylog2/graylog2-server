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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

public class RawCodec implements Codec {
    private final ObjectMapper objectMapper;
    private final Configuration configuration;

    @AssistedInject
    public RawCodec(ObjectMapper objectMapper, @Assisted Configuration configuration) {
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage raw) {
        // TODO fix remote address, use raw message
        String source;
        if (raw.getMetaData() != null) {
            try {
                final Map<String, Object> map = objectMapper.readValue(raw.getMetaData(),
                                                        new TypeReference<Map<String, Object>>() {
                                                        });
                source = map.containsKey("remote_address") ? (String) map.get("remote_address") : "unknown";
            } catch (IOException e) {
                source = "unknown";
            }
        } else {
            if (configuration.stringIsSet(MessageInput2.CK_OVERRIDE_SOURCE)) {
                source = configuration.getString(MessageInput2.CK_OVERRIDE_SOURCE);
            } else {
                source = "unknown";
            }
        }
        return new Message(new String(raw.getPayload(), Charsets.UTF_8), source, raw.getTimestamp());
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

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        return new ConfigurationRequest();
    }

    public interface Factory extends Codec.Factory<RawCodec> {
        @Override
        RawCodec create(Configuration configuration);
    }
}
