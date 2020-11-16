/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.messageprocessors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class MessageProcessorsConfig {
    @JsonProperty("processor_order")
    public abstract List<String> processorOrder();

    @JsonProperty("disabled_processors")
    public abstract Set<String> disabledProcessors();

    @JsonCreator
    public static MessageProcessorsConfig create(@JsonProperty("processor_order") List<String> processorOrder,
                                                 @JsonProperty("disabled_processors") Set<String> disabledProcessors) {
        return builder()
                .processorOrder(processorOrder)
                .disabledProcessors(disabledProcessors)
                .build();
    }

    public static MessageProcessorsConfig create(List<String> processorOrder) {
        return builder()
                .processorOrder(processorOrder)
                .disabledProcessors(Collections.emptySet())
                .build();
    }

    public static MessageProcessorsConfig defaultConfig() {
        return builder()
                .processorOrder(Collections.emptyList())
                .disabledProcessors(Collections.emptySet())
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MessageProcessorsConfig.Builder();
    }

    public abstract Builder toBuilder();

    public MessageProcessorsConfig withProcessors(final Set<String> availableProcessors) {
        final List<String> newOrder = new ArrayList<>();

        // Check if processor actually exists.
        processorOrder().stream()
                .filter(availableProcessors::contains)
                .forEach(newOrder::add);

        // Add availableProcessors which are not in the config yet to the end.
        availableProcessors.stream()
                .filter(processor -> !newOrder.contains(processor))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(newOrder::add);

        return toBuilder().processorOrder(newOrder).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder processorOrder(List<String> processorOrder);

        public abstract Builder disabledProcessors(Set<String> disabledProcessors);

        public abstract MessageProcessorsConfig build();
    }
}
