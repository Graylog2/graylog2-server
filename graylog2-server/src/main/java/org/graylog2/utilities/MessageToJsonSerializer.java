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
package org.graylog2.utilities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MessageToJsonSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(MessageToJsonSerializer.class);

    private final ObjectMapper mapper;
    private final SimpleModule simpleModule;
    private final StreamService streamService;
    private final InputService inputService;
    private final LoadingCache<String, Stream> streamCache;
    private final LoadingCache<String, Optional<MessageInput>> messageInputCache;

    private static class SerializeBean {
        private final Message message;

        public SerializeBean(Message message) {
            this.message = message;
        }

        @JsonProperty("fields")
        public Map<String, Object> getFields() {
            return message.getFields();
        }

        @JsonProperty("streams")
        public List<String> getStreams() {
            final List<String> list = Lists.newArrayList();

            if (! message.getStreams().isEmpty()) {
                for (Stream stream : message.getStreams()) {
                    list.add(stream.getId());
                }
            }

            return list;
        }

        @JsonProperty("source_input")
        public String getSourceInput() {
            if (message.getSourceInput() != null) {
                return message.getSourceInput().getId();
            } else {
                return null;
            }
        }
    }

    private static class DeserializeBean {
        private Map<String, Object> fields;
        private List<String> streams;
        private String sourceInput;

        public Map<String, Object> getFields() {
            return fields;
        }

        @JsonProperty("fields")
        public void setFields(Map<String, Object> fields) {
            this.fields = fields;
        }

        public List<String> getStreams() {
            return streams;
        }

        @JsonProperty("streams")
        public void setStreams(List<String> streams) {
            this.streams = streams;
        }

        public String getSourceInput() {
            return sourceInput;
        }

        @JsonProperty("source_input")
        public void setSourceInput(String sourceInput) {
            this.sourceInput = sourceInput;
        }
    }

    private static class NodeIdSerializer extends JsonSerializer<NodeId> {
        @Override
        public Class<NodeId> handledType() {
            return NodeId.class;
        }

        @Override
        public void serialize(NodeId value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(value.toString());
        }
    }

    @Inject
    public MessageToJsonSerializer(final ObjectMapper globalMapper, final StreamService streamService, final InputService inputService) {
        // Make sure to copy the mapper so we can customize it later without affecting the injected one.
        this.mapper = globalMapper.copy();
        this.streamService = streamService;
        this.inputService = inputService;
        this.streamCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, Stream>() {
                            @Override
                            public Stream load(String key) throws Exception {
                                // TODO This might create lots of Stream instances. Can we avoid this?
                                LOG.debug("Loading stream {}", key);
                                return streamService.load(key);
                            }
                        }
                );
        this.messageInputCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, Optional<MessageInput>>() {
                            @Override
                            public Optional<MessageInput> load(String key) throws Exception {
                                LOG.debug("Loading message input {}", key);
                                try {
                                    final Input input = inputService.find(key);
                                    return Optional.fromNullable(inputService.buildMessageInput(input));
                                } catch (NotFoundException | NoSuchInputTypeException e) {
                                    return Optional.absent();
                                }
                            }
                        }
                );
        simpleModule = new SimpleModule() {
            {
                addSerializer(new NodeIdSerializer());
            }
        };
        this.mapper.registerModule(simpleModule);
        this.mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public byte[] serializeToBytes(Message message) throws JsonProcessingException {
        return mapper.writeValueAsBytes(new SerializeBean(message));
    }

    public String serializeToString(Message message) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new SerializeBean(message));
    }

    public Message deserialize(byte[] bytes) throws IOException {
        final DeserializeBean bean = mapper.readValue(bytes, DeserializeBean.class);
        final Map<String, Object> fields = bean.getFields();
        final Message message = new Message(
                (String) fields.remove("message"),
                (String) fields.remove("source"),
                new DateTime((long) fields.remove("timestamp"), DateTimeZone.UTC)
        );
        final List<Stream> streamList = Lists.newArrayList();

        for (String id : bean.getStreams()) {
            Stream stream = getStream(id);

            if (stream != null) {
                streamList.add(stream);
            }
        }

        message.setStreams(streamList);
        message.addFields(fields);

        final MessageInput input;
        if (bean.getSourceInput() != null)
            input = getMessageInput(bean.getSourceInput());
        else
            input = null;

        if (input != null) {
            message.setSourceInput(input);
        }

        return message;
    }

    public Message deserialize(String string) throws IOException {
        return deserialize(string.getBytes(StandardCharsets.UTF_8));
    }

    private Stream getStream(String id) {
        try {
            return streamCache.get(id);
        } catch (ExecutionException e) {
            LOG.error("Stream cache error", e);
            return null;
        }
    }

    private MessageInput getMessageInput(String id) {
        try {
            final Optional<MessageInput> cacheResult = messageInputCache.get(id);
            return cacheResult.orNull();
        } catch (ExecutionException e) {
            LOG.error("Message input cache error", e);
            return null;
        }
    }

}
