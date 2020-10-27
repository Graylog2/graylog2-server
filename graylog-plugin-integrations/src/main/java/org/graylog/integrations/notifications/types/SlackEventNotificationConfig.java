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

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
@JsonTypeName(SlackEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = SlackEventNotificationConfig.Builder.class)
public abstract class SlackEventNotificationConfig implements EventNotificationConfig {

	private final String regex = "https:\\/\\/hooks.slack.com\\/services\\/";
	private final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
	public static final String TYPE_NAME = "slack-notification-v1";

	static final String FIELD_COLOR = "color";
	static final String FIELD_WEBHOOK_URL = "webhook_url";
	static final String FIELD_CHANNEL = "channel";
	static final String FIELD_CUSTOM_MESSAGE = "custom_message";
	static final String FIELD_USER_NAME = "user_name";
	static final String FIELD_NOTIFY_CHANNEL = "notify_channel";
	static final String FIELD_LINK_NAMES = "link_names";
	static final String FIELD_ICON_URL = "icon_url";
	static final String FIELD_ICON_EMOJI = "icon_emoji";


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

	@JsonProperty(FIELD_USER_NAME)
	@Nullable
	public abstract String userName();

	@JsonProperty(FIELD_NOTIFY_CHANNEL)
	public abstract boolean notifyChannel();

	@JsonProperty(FIELD_LINK_NAMES)
	public abstract boolean linkNames();

	@JsonProperty(FIELD_ICON_URL)
	@Nullable
	public abstract String iconUrl();

	@JsonProperty(FIELD_ICON_EMOJI)
	@Nullable
	public abstract String iconEmoji();


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
		ValidationResult validation =  new ValidationResult();
		final Matcher matcher = pattern.matcher(webhookUrl());

		if (channel().isEmpty()) {
			validation.addError(FIELD_CHANNEL, "Channel cannot be empty.");
		}

		if(matcher.find() == false) {
			validation.addError(FIELD_WEBHOOK_URL, "please specify a valid webhook url");
		}
		return validation;

	}

	@AutoValue.Builder
	public static abstract class Builder implements EventNotificationConfig.Builder<SlackEventNotificationConfig.Builder> {
		@JsonCreator
		public static SlackEventNotificationConfig.Builder create() {

			return new AutoValue_SlackEventNotificationConfig.Builder()
					.type(TYPE_NAME)
					.color("#ff0500")
					.webhookUrl("https://hooks.slack.com/services/xxx/xxxx/xxxxxxxxxxxxxxxxxxx")
					.channel("slacktest2")
					.customMessage("hello World")
					.notifyChannel(false)
					.linkNames(false);
		}

		@JsonProperty(FIELD_COLOR)
		public abstract SlackEventNotificationConfig.Builder color(String color);

		@JsonProperty(FIELD_WEBHOOK_URL)
		public abstract SlackEventNotificationConfig.Builder webhookUrl(String webhookUrl);

		@JsonProperty(FIELD_CHANNEL)
		public abstract SlackEventNotificationConfig.Builder channel(String channel);

		@JsonProperty(FIELD_CUSTOM_MESSAGE)
		public abstract SlackEventNotificationConfig.Builder customMessage(String customMessage);

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


		public abstract SlackEventNotificationConfig build();
	}

	@Override
	public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
		return SlackEventNotificationConfigEntity.builder()
				.color(ValueReference.of(color()))
				.webhookUrl(ValueReference.of(webhookUrl()))
				.channel(ValueReference.of(channel()))
				.customMessage(ValueReference.of(customMessage()))
				.userName(ValueReference.of(userName()))
				.notifyChannel(ValueReference.of(notifyChannel()))
				.linkNames(ValueReference.of(linkNames()))
				.iconUrl(ValueReference.of(iconUrl()))
				.iconEmoji(ValueReference.of(iconEmoji()))
				.build();
	}
}
