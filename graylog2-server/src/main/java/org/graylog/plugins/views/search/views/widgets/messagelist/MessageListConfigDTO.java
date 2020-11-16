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
package org.graylog.plugins.views.search.views.widgets.messagelist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.SortConfigDTO;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoValue
@JsonTypeName(MessageListConfigDTO.NAME)
@JsonDeserialize(builder = MessageListConfigDTO.Builder.class)
public abstract class MessageListConfigDTO implements WidgetConfigDTO {
    public static final String NAME = "messages";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_SHOW_MESSAGE_ROW = "show_message_row";
    private static final String FIELD_DECORATORS = "decorators";
    private static final String FIELD_SORT = "sort";

    @JsonProperty(FIELD_FIELDS)
    public abstract ImmutableSet<String> fields();

    @JsonProperty(FIELD_SHOW_MESSAGE_ROW)
    public abstract boolean showMessageRow();

    @JsonProperty(FIELD_DECORATORS)
    public abstract List<Decorator> decorators();

    @JsonProperty(FIELD_SORT)
    public abstract List<SortConfigDTO> sort();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_FIELDS)
        public abstract Builder fields(ImmutableSet<String> fields);

        @JsonProperty(FIELD_SHOW_MESSAGE_ROW)
        public abstract Builder showMessageRow(boolean showMessageRow);

        @JsonProperty(FIELD_DECORATORS)
        public Builder _decorators(List<DecoratorImpl> decorators) {
            return decorators(new ArrayList<>(decorators));
        }
        public abstract Builder decorators(List<Decorator> decorators);

        @JsonProperty(FIELD_SORT)
        public abstract Builder sort(List<SortConfigDTO> sort);

        public abstract MessageListConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_MessageListConfigDTO.Builder()
                    .decorators(Collections.emptyList())
                    .sort(Collections.emptyList());
        }
    }
}
