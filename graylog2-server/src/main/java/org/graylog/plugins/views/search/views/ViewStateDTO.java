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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = ViewStateDTO.Builder.class)
@WithBeanGetter
public abstract class ViewStateDTO implements ContentPackable<ViewStateEntity> {
    static final String FIELD_SELECTED_FIELDS = "selected_fields";
    static final String FIELD_STATIC_MESSAGE_LIST_ID = "static_message_list_id";
    static final String FIELD_TITLES = "titles";
    static final String FIELD_WIDGETS = "widgets";
    static final String FIELD_WIDGET_MAPPING = "widget_mapping";
    static final String FIELD_WIDGET_POSITIONS = "positions";
    static final String FIELD_FORMATTING = "formatting";
    static final String FIELD_DISPLAY_MODE_SETTINGS = "display_mode_settings";

    @Nullable
    @JsonProperty(FIELD_SELECTED_FIELDS)
    public abstract Optional<Set<String>> fields();

    @Nullable
    @JsonProperty(FIELD_STATIC_MESSAGE_LIST_ID)
    public abstract Optional<String> staticMessageListId();

    @JsonProperty(FIELD_TITLES)
    public abstract Titles titles();

    @JsonProperty(FIELD_WIDGETS)
    public abstract Set<WidgetDTO> widgets();

    @JsonProperty(FIELD_WIDGET_MAPPING)
    public abstract Map<String, Set<String>> widgetMapping();

    @JsonProperty(FIELD_WIDGET_POSITIONS)
    public abstract Map<String, WidgetPositionDTO> widgetPositions();

    @JsonProperty(FIELD_FORMATTING)
    @Nullable
    public abstract FormattingSettings formatting();

    @JsonProperty(FIELD_DISPLAY_MODE_SETTINGS)
    public abstract DisplayModeSettings displayModeSettings();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @Nullable
        @JsonProperty(FIELD_SELECTED_FIELDS)
        public abstract Builder fields(Set<String> fields);

        @Nullable
        @JsonProperty(FIELD_STATIC_MESSAGE_LIST_ID)
        public abstract Builder staticMessageListId(String staticMessageListId);

        @JsonProperty(FIELD_TITLES)
        public abstract Builder titles(Titles titles);

        @JsonProperty(FIELD_WIDGETS)
        public abstract Builder widgets(Set<WidgetDTO> widgets);

        @JsonProperty(FIELD_WIDGET_MAPPING)
        public abstract Builder widgetMapping(Map<String, Set<String>> widgetMapping);

        @JsonProperty(FIELD_WIDGET_POSITIONS)
        public abstract Builder widgetPositions(Map<String, WidgetPositionDTO> widgetPositions);

        @JsonProperty(FIELD_FORMATTING)
        public abstract Builder formatting(FormattingSettings formattingSettings);

        @JsonProperty(FIELD_DISPLAY_MODE_SETTINGS)
        public abstract Builder displayModeSettings(DisplayModeSettings displayModeSettings);

        public abstract ViewStateDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewStateDTO.Builder()
                    .titles(Titles.empty())
                    .displayModeSettings(DisplayModeSettings.empty());
        }
    }

    @Override
    public ViewStateEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        final ViewStateEntity.Builder viewStateBuilder = ViewStateEntity.builder()
                .titles(this.titles())
                .displayModeSettings(this.displayModeSettings())
                .formatting(this.formatting())
                .widgets(this.widgets().stream().map(widgetDTO -> widgetDTO.toContentPackEntity(entityDescriptorIds))
                        .collect(Collectors.toSet()))
                .widgetPositions(this.widgetPositions())
                .widgetMapping(this.widgetMapping());
        if (this.fields() != null && this.fields().isPresent()) {
            viewStateBuilder.fields(this.fields().get());
        }
        if (this.staticMessageListId() != null && this.staticMessageListId().isPresent()) {
            viewStateBuilder.staticMessageListId(this.staticMessageListId().get());
        }
        return viewStateBuilder.build();
    }
}
