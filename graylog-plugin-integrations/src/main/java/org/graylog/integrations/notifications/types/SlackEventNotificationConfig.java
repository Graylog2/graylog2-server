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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import javax.validation.constraints.NotBlank;

@AutoValue
@JsonTypeName(SlackEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = SlackEventNotificationConfig.Builder.class)
public abstract class SlackEventNotificationConfig implements EventNotificationConfig {
	public static final String TYPE_NAME = "slack-notification-v1";

	static final String FIELD_COLOR = "color";
	static final String FIELD_WEBHOOK_URL = "webhook_url";
	static final String FIELD_CHANNEL = "channel";
	static final String FIELD_CUSTOM_MESSAGE = "custom_message";
	static final String FIELD_BACKLOG_ITEM_MESSAGE = "backlog_item_message";
	static final String FIELD_USER_NAME = "user_name";
	static final String FIELD_NOTIFY_CHANNEL = "notify_channel";
	static final String FIELD_LINK_NAMES = "link_names";
	static final String FIELD_ICON_URL = "icon_url";
	static final String FIELD_ICON_EMOJI = "icon_emoji";
	static final String FIELD_GRAYLOG_URL = "graylog_url";


    // TODO: 9/8/20
	//See my comment in the SlackClient. We should use the pre-configured okhttp client so we automatically get
    // the correct proxy configuration.
    // The proxy setting can be removed once we switched to okhttp.
	static final String FIELD_PROXY = "proxy";

	@JsonProperty(FIELD_COLOR)
	@NotBlank
	public abstract String color();

	@JsonProperty(FIELD_WEBHOOK_URL)
	@NotBlank
	public abstract String webhookUrl();

	@JsonProperty(FIELD_CHANNEL)
	@NotBlank
	public abstract String channel();

	@JsonProperty(FIELD_CUSTOM_MESSAGE)
	public abstract String customMessage();

	@JsonProperty(FIELD_BACKLOG_ITEM_MESSAGE)
	public abstract String backlogItemMessage();

	@JsonProperty(FIELD_USER_NAME)
	public abstract String userName();

	@JsonProperty(FIELD_NOTIFY_CHANNEL)
	public abstract boolean notifyChannel();

	@JsonProperty(FIELD_LINK_NAMES)
	public abstract boolean linkNames();

	@JsonProperty(FIELD_ICON_URL)
	public abstract String iconUrl();

	@JsonProperty(FIELD_ICON_EMOJI)
	public abstract String iconEmoji();

	@JsonProperty(FIELD_GRAYLOG_URL)
	public abstract String graylogUrl();

	@JsonProperty(FIELD_PROXY)
	public abstract String proxy();

	@Override
	@JsonIgnore
	public JobTriggerData toJobTriggerData(EventDto dto) {
		return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
	}

	public static SlackEventNotificationConfig.Builder builder() {
		return SlackEventNotificationConfig.Builder.create();
	}

	@Override
	@JsonIgnore
	public ValidationResult validate() {
		return new ValidationResult();
	}

	@AutoValue.Builder
	public static abstract class Builder implements EventNotificationConfig.Builder<SlackEventNotificationConfig.Builder> {
		@JsonCreator
		public static SlackEventNotificationConfig.Builder create() {
			return new AutoValue_SlackEventNotificationConfig.Builder()
					.type(TYPE_NAME);
		}

		@JsonProperty(FIELD_COLOR)
		public abstract SlackEventNotificationConfig.Builder color(String color);

		@JsonProperty(FIELD_WEBHOOK_URL)
		public abstract SlackEventNotificationConfig.Builder webhookUrl(String webhookUrl);

		@JsonProperty(FIELD_CHANNEL)
		public abstract SlackEventNotificationConfig.Builder channel(String channel);

		@JsonProperty(FIELD_CUSTOM_MESSAGE)
		public abstract SlackEventNotificationConfig.Builder customMessage(String customMessage);

		@JsonProperty(FIELD_BACKLOG_ITEM_MESSAGE)
		public abstract SlackEventNotificationConfig.Builder backlogItemMessage(String backlogItemMessage);

		@JsonProperty(FIELD_USER_NAME)
		public abstract SlackEventNotificationConfig.Builder userName(String userName);

		@JsonProperty(FIELD_NOTIFY_CHANNEL)
		public abstract SlackEventNotificationConfig.Builder notifyChannel(boolean notifyChannel);

		@JsonProperty(FIELD_LINK_NAMES)
		public abstract SlackEventNotificationConfig.Builder linkNames(boolean linkNames);

		@JsonProperty(FIELD_ICON_URL)
		public abstract SlackEventNotificationConfig.Builder iconUrl(String iconUrl);

		@JsonProperty(FIELD_ICON_EMOJI)
		public abstract SlackEventNotificationConfig.Builder iconEmoji(String iconEmoji);

		@JsonProperty(FIELD_GRAYLOG_URL)
		public abstract SlackEventNotificationConfig.Builder graylogUrl(String graylogUrl);

		@JsonProperty(FIELD_PROXY)
		public abstract SlackEventNotificationConfig.Builder proxy(String proxy);

		public abstract SlackEventNotificationConfig build();
	}

	@Override
	public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
		return SlackEventNotificationConfigEntity.builder()
				.color(ValueReference.of(color()))
				.webhookUrl(ValueReference.of(webhookUrl()))
				.channel(ValueReference.of(channel()))
				.customMessage(ValueReference.of(customMessage()))
				.backlogItemMessage(ValueReference.of(backlogItemMessage()))
				.userName(ValueReference.of(userName()))
				.notifyChannel(ValueReference.of(notifyChannel()))
				.linkNames(ValueReference.of(linkNames()))
				.iconUrl(ValueReference.of(iconUrl()))
				.iconEmoji(ValueReference.of(iconEmoji()))
				.graylogUrl(ValueReference.of(graylogUrl()))
				.proxy(ValueReference.of(proxy()))
				.build();
	}
}
