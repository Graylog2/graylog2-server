/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.messageprocessors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.messageprocessors.MessageProcessor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class MessageProcessorsConfigWithDescriptors {
    @JsonProperty("processor_order")
    public abstract List<MessageProcessorDescriptor> processorOrder();

    @JsonProperty("disabled_processors")
    public abstract Set<String> disabledProcessors();

    @JsonCreator
    public static MessageProcessorsConfigWithDescriptors create(@JsonProperty("processor_order") List<MessageProcessorDescriptor> processorOrder,
                                                                @JsonProperty("disabled_processors") Set<String> disabledProcessors) {
        return builder()
                .processorOrder(processorOrder)
                .disabledProcessors(disabledProcessors)
                .build();
    }

    public static MessageProcessorsConfigWithDescriptors fromConfig(MessageProcessorsConfig config,
                                                                    Set<MessageProcessor.Descriptor> descriptors) {
        final Map<String, MessageProcessor.Descriptor> descriptorMap = descriptors.stream()
                .collect(Collectors.toMap(MessageProcessor.Descriptor::className, descriptor -> descriptor));

        return builder()
                .processorOrder(config.processorOrder().stream()
                        .map(s -> MessageProcessorDescriptor.fromDescriptor(descriptorMap.get(s)))
                        .collect(Collectors.toList()))
                .disabledProcessors(config.disabledProcessors())
                .build();
    }

    public MessageProcessorsConfig toConfig() {
        return MessageProcessorsConfig.builder()
                .processorOrder(processorOrder().stream()
                        .map(MessageProcessorDescriptor::className)
                        .collect(Collectors.toList()))
                .disabledProcessors(disabledProcessors())
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MessageProcessorsConfigWithDescriptors.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder processorOrder(List<MessageProcessorDescriptor> processorOrder);

        public abstract Builder disabledProcessors(Set<String> disabledMessageProcessors);

        public abstract MessageProcessorsConfigWithDescriptors build();
    }
}