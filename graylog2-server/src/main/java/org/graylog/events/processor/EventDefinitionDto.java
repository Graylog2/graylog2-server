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
package org.graylog.events.processor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = EventDefinitionDto.Builder.class)
@WithBeanGetter
public abstract class EventDefinitionDto implements EventDefinition {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_ALERT = "alert";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_FIELD_SPEC = "field_spec";
    private static final String FIELD_KEY_SPEC = "key_spec";
    private static final String FIELD_NOTIFICATION_SETTINGS = "notification_settings";
    private static final String FIELD_NOTIFICATIONS = "notifications";
    private static final String FIELD_STORAGE = "storage";

    @Override
    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @Override
    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @Override
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @Override
    @JsonProperty(FIELD_PRIORITY)
    public abstract int priority();

    @Override
    @JsonProperty(FIELD_ALERT)
    public abstract boolean alert();

    @Override
    @JsonProperty(FIELD_CONFIG)
    public abstract EventProcessorConfig config();

    @Override
    @JsonProperty(FIELD_FIELD_SPEC)
    public abstract ImmutableMap<String, EventFieldSpec> fieldSpec();

    @Override
    @JsonProperty(FIELD_KEY_SPEC)
    public abstract ImmutableList<String> keySpec();

    @Override
    @JsonProperty(FIELD_NOTIFICATION_SETTINGS)
    public abstract EventNotificationSettings notificationSettings();

    @Override
    @JsonProperty(FIELD_NOTIFICATIONS)
    public abstract ImmutableList<EventNotificationHandler.Config> notifications();

    @Override
    @JsonProperty(FIELD_STORAGE)
    public abstract ImmutableList<EventStorageHandler.Config> storage();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventDefinitionDto.Builder()
                    .fieldSpec(ImmutableMap.of())
                    .notifications(ImmutableList.of())
                    .storage(ImmutableList.of());
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_PRIORITY)
        public abstract Builder priority(int priority);

        @JsonProperty(FIELD_ALERT)
        public abstract Builder alert(boolean alert);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(EventProcessorConfig config);

        @JsonProperty(FIELD_FIELD_SPEC)
        public abstract Builder fieldSpec(ImmutableMap<String, EventFieldSpec> fieldSpec);

        @JsonProperty(FIELD_KEY_SPEC)
        public abstract Builder keySpec(ImmutableList<String> keySpec);

        @JsonProperty(FIELD_NOTIFICATION_SETTINGS)
        public abstract Builder notificationSettings(EventNotificationSettings notificationSettings);

        @JsonProperty(FIELD_NOTIFICATIONS)
        public abstract Builder notifications(ImmutableList<EventNotificationHandler.Config> notifications);

        @JsonProperty(FIELD_STORAGE)
        public abstract Builder storage(ImmutableList<EventStorageHandler.Config> storageHandlers);

        abstract EventDefinitionDto autoBuild();

        public EventDefinitionDto build() {
            final EventDefinitionDto dto = autoBuild();
            final PersistToStreamsStorageHandler.Config withDefaultEventsStream = PersistToStreamsStorageHandler.Config.createWithDefaultEventsStream();

            if (dto.storage().stream().noneMatch(withDefaultEventsStream::equals)) {
                final List<EventStorageHandler.Config> handlersWithoutPersistToStreams = dto.storage().stream()
                        // We don't allow custom persist-to-streams handlers at the moment
                        .filter(handler -> !PersistToStreamsStorageHandler.Config.TYPE_NAME.equals(handler.type()))
                        .collect(Collectors.toList());

                return dto.toBuilder()
                        // Right now we always want to persist events into the default events stream
                        .storage(ImmutableList.<EventStorageHandler.Config>builder()
                                .addAll(handlersWithoutPersistToStreams)
                                .add(withDefaultEventsStream)
                                .build())
                        .build();
            }

            return dto;
        }
    }
}
