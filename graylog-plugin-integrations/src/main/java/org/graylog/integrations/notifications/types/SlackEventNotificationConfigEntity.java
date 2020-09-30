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
package org.graylog.integrations.notifications.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

@AutoValue
@JsonTypeName(SlackEventNotificationConfigEntity.TYPE_NAME)
@JsonDeserialize(builder = SlackEventNotificationConfigEntity.Builder.class)
public abstract class SlackEventNotificationConfigEntity implements EventNotificationConfigEntity {

	public static final String TYPE_NAME = "slack-notification-v1";

	@JsonProperty(SlackEventNotificationConfig.FIELD_COLOR)
	public abstract ValueReference color();

	@JsonProperty(SlackEventNotificationConfig.FIELD_WEBHOOK_URL)
	public abstract ValueReference webhookUrl();

	@JsonProperty(SlackEventNotificationConfig.FIELD_CHANNEL)
	public abstract ValueReference channel();

	@JsonProperty(SlackEventNotificationConfig.FIELD_CUSTOM_MESSAGE)
	public abstract ValueReference customMessage();

	@JsonProperty(SlackEventNotificationConfig.FIELD_BACKLOG_ITEM_MESSAGE)
	public abstract ValueReference backlogItemMessage();

	@JsonProperty(SlackEventNotificationConfig.FIELD_USER_NAME)
	public abstract ValueReference userName();

	@JsonProperty(SlackEventNotificationConfig.FIELD_NOTIFY_CHANNEL)
	public abstract ValueReference notifyChannel();

	@JsonProperty(SlackEventNotificationConfig.FIELD_LINK_NAMES)
	public abstract ValueReference linkNames();

	@JsonProperty(SlackEventNotificationConfig.FIELD_ICON_URL)
	public abstract ValueReference iconUrl();

	@JsonProperty(SlackEventNotificationConfig.FIELD_ICON_EMOJI)
	public abstract ValueReference iconEmoji();

	@JsonProperty(SlackEventNotificationConfig.FIELD_GRAYLOG_URL)
	public abstract ValueReference graylogUrl();

	@JsonProperty(SlackEventNotificationConfig.FIELD_PROXY)
	public abstract ValueReference proxy();

	public static Builder builder() {
		return Builder.create();
	}

	public abstract Builder toBuilder();

	@AutoValue.Builder
	public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

		@JsonCreator
		public static Builder create() {
			return new AutoValue_SlackEventNotificationConfigEntity.Builder()
					.type(TYPE_NAME);
		}

		@JsonProperty(SlackEventNotificationConfig.FIELD_COLOR)
		public abstract Builder color(ValueReference color);

		@JsonProperty(SlackEventNotificationConfig.FIELD_WEBHOOK_URL)
		public abstract Builder webhookUrl(ValueReference webhookUrl);

		@JsonProperty(SlackEventNotificationConfig.FIELD_CHANNEL)
		public abstract Builder channel(ValueReference channel);

		@JsonProperty(SlackEventNotificationConfig.FIELD_CUSTOM_MESSAGE)
		public abstract Builder customMessage(ValueReference customMessage);

		@JsonProperty(SlackEventNotificationConfig.FIELD_BACKLOG_ITEM_MESSAGE)
		public abstract Builder backlogItemMessage(ValueReference backlogItemMessage);

		@JsonProperty(SlackEventNotificationConfig.FIELD_USER_NAME)
		public abstract Builder userName(ValueReference userName);

		@JsonProperty(SlackEventNotificationConfig.FIELD_NOTIFY_CHANNEL)
		public abstract Builder notifyChannel(ValueReference notifyChannel);

		@JsonProperty(SlackEventNotificationConfig.FIELD_LINK_NAMES)
		public abstract Builder linkNames(ValueReference linkNames);

		@JsonProperty(SlackEventNotificationConfig.FIELD_ICON_URL)
		public abstract Builder iconUrl(ValueReference iconUrl);

		@JsonProperty(SlackEventNotificationConfig.FIELD_ICON_EMOJI)
		public abstract Builder iconEmoji(ValueReference iconEmoji);

		@JsonProperty(SlackEventNotificationConfig.FIELD_GRAYLOG_URL)
		public abstract Builder graylogUrl(ValueReference graylogUrl);

		@JsonProperty(SlackEventNotificationConfig.FIELD_PROXY)
		public abstract Builder proxy(ValueReference proxy);

		public abstract SlackEventNotificationConfigEntity build();
	}

	@Override
	public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
		return SlackEventNotificationConfig.builder()
				.color(color().asString(parameters))
				.webhookUrl(webhookUrl().asString(parameters))
				.channel(channel().asString(parameters))
				.customMessage(customMessage().asString(parameters))
				.backlogItemMessage(backlogItemMessage().asString(parameters))
				.userName(userName().asString(parameters))
				.notifyChannel(notifyChannel().asBoolean(parameters))
				.linkNames(linkNames().asBoolean(parameters))
				.iconUrl(iconUrl().asString(parameters))
				.iconEmoji(iconEmoji().asString(parameters))
				.graylogUrl(graylogUrl().asString(parameters))
				.proxy(proxy().asString(parameters))
				.build();
	}
}
