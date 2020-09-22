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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.floreysoft.jmte.Engine;
import org.apache.commons.lang3.StringUtils;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.modeldata.BacklogItemModelData;
import org.graylog.events.notifications.modeldata.CustomMessageModelData;
import org.graylog.events.notifications.modeldata.StreamModelData;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackEventNotification implements EventNotification {

	private static final String UNKNOWN_VALUE = "<unknown>";

	public interface Factory extends EventNotification.Factory {
		@Override
        SlackEventNotification create();
	}

	private static final Logger LOG = LoggerFactory.getLogger(SlackEventNotification.class);

	private final EventNotificationService notificationCallbackService;
	private final StreamService streamService;
	private final Engine templateEngine;
	private final NotificationService notificationService;
	private final ObjectMapper objectMapper;
	private final NodeId nodeId;
	private final SlackClient slackClient;

	@Inject
	public SlackEventNotification(EventNotificationService notificationCallbackService,
                                  SlackClient slackClient,
                                  StreamService streamService,
                                  Engine templateEngine,
                                  NotificationService notificationService,
                                  ObjectMapper objectMapper,
                                  NodeId nodeId) {
		this.notificationCallbackService = notificationCallbackService;
		this.streamService = streamService;
		this.templateEngine = templateEngine;
		this.notificationService = notificationService;
		this.objectMapper = objectMapper;
		this.nodeId = nodeId;
		this.slackClient = slackClient;

	}

	@Override
	public void execute(EventNotificationContext ctx) throws PermanentEventNotificationException {
		final SlackEventNotificationConfig config = (SlackEventNotificationConfig) ctx.notificationConfig();
		// TODO: 9/8/20  - use this.slackClient
        //
        SlackClient slackClient = new SlackClient(config);

		try {
			SlackMessage slackMessage = createSlackMessage(ctx, config);
			slackClient.send(slackMessage);
		} catch (Exception e) {
			String exceptionDetail = e.toString();
			if (e.getCause() != null) {
				exceptionDetail += " (" + e.getCause() + ")";
			}

			final Notification systemNotification = notificationService.buildNow()
					.addNode(nodeId.toString())
					.addType(Notification.Type.GENERIC)
					.addSeverity(Notification.Severity.NORMAL)
					.addDetail("exception", exceptionDetail);
			notificationService.publishIfFirst(systemNotification);

			throw new PermanentEventNotificationException("Slack notification is triggered, but sending failed. " + e.getMessage(), e);
		}
	}

	private SlackMessage createSlackMessage(EventNotificationContext ctx, SlackEventNotificationConfig config) {
		//Note: Link names if notify channel or else the channel tag will be plain text.
		boolean linkNames = config.linkNames() || config.notifyChannel();
		String message = buildDefaultMessage(ctx, config);

		String customMessage = null;
		String template = config.customMessage();
		boolean hasTemplate = !isNullOrEmpty(template);
		if (hasTemplate) {
			customMessage = buildCustomMessage(ctx, config, template);
		}

		List<String> backlogItemMessages = Collections.emptyList();
		String backlogItemTemplate = config.backlogItemMessage();
		boolean hasBacklogItemTemplate = !isNullOrEmpty(backlogItemTemplate);
		if(hasBacklogItemTemplate) {
			backlogItemMessages = buildBacklogItemMessages(ctx, config, backlogItemTemplate);
		}

		return new SlackMessage(
				config.color(),
				config.iconEmoji(),
				config.iconUrl(),
				config.userName(),
				config.channel(),
				linkNames,
				message,
				customMessage,
				backlogItemMessages);
	}

	private String buildDefaultMessage(EventNotificationContext ctx, SlackEventNotificationConfig config) {
		String title = buildMessageTitle(ctx, config);

		// Build custom message
		String audience = config.notifyChannel() ? "@channel " : "";
		String description = ctx.eventDefinition().map(EventDefinitionDto::description).orElse("");
		return String.format(Locale.ROOT,"%s*Alert %s* triggered:\n> %s \n", audience, title, description);
	}

	private String buildMessageTitle(EventNotificationContext ctx, SlackEventNotificationConfig config) {
		String graylogUrl = config.graylogUrl();
		String eventDefinitionName = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Unnamed");
		if(!isNullOrEmpty(graylogUrl)) {
			return "<" + graylogUrl + "|" + eventDefinitionName + ">";
		} else {
			return "_" + eventDefinitionName + "_";
		}
	}

	private String buildCustomMessage(EventNotificationContext ctx, SlackEventNotificationConfig config, String template) {
		List<MessageSummary> backlog = getAlarmBacklog(ctx);
		Map<String, Object> model = getCustomMessageModel(ctx, config, backlog);
		try {
			return templateEngine.transform(template, model);
		} catch (Exception e) {
			LOG.error("Exception during templating", e);
			return e.toString();
		}
	}

	private List<String> buildBacklogItemMessages(EventNotificationContext ctx, SlackEventNotificationConfig config, String template) {
		return getAlarmBacklog(ctx).stream()
				.map(backlogItem -> {
					Map<String, Object> model = getBacklogItemModel(ctx, config, backlogItem);
					try {
						return templateEngine.transform(template, model);
					} catch (Exception e) {
						LOG.error("Exception during templating", e);
						return e.toString();
					}
				}).collect(Collectors.toList());
	}

	private List<MessageSummary> getAlarmBacklog(EventNotificationContext ctx) {
		return notificationCallbackService.getBacklogForEvent(ctx);
	}

	private Map<String, Object> getCustomMessageModel(EventNotificationContext ctx, SlackEventNotificationConfig config, List<MessageSummary> backlog) {
		Optional<EventDefinitionDto> definitionDto = ctx.eventDefinition();

		List<StreamModelData> streams = streamService.loadByIds(ctx.event().sourceStreams())
				.stream()
				.map(stream -> buildStreamWithUrl(stream, ctx, config))
				.collect(Collectors.toList());

		// TODO: 9/8/20
        /**
         *
         It looks like we are duplicating a lot of code from EventNotificationModelData in the CustomMessageModelData
         and BacklogItemModelData value objects to expose some more attributes to the slack message template.
         Both objects then get converted into a Map<String, Object> immediately. wink

         How about using the regular EventNotificationModelData,
         convert that into a Map<String, Object> and then manually add the additional attributes we need?
         That way we would save a lot of duplicated code.

         Also, if we add additional attributes here that would also be useful for all notifications,
         we can think about extending EventNotificationModelData.
         */

		CustomMessageModelData modelData = CustomMessageModelData.builder()
				.eventDefinition(definitionDto)
				.eventDefinitionId(definitionDto.map(EventDefinitionDto::id).orElse(UNKNOWN_VALUE))
				.eventDefinitionType(definitionDto.map(d -> d.config().type()).orElse(UNKNOWN_VALUE))
				.eventDefinitionTitle(definitionDto.map(EventDefinitionDto::title).orElse(UNKNOWN_VALUE))
				.eventDefinitionDescription(definitionDto.map(EventDefinitionDto::description).orElse(UNKNOWN_VALUE))
				.jobDefinitionId(ctx.jobTrigger().map(JobTriggerDto::jobDefinitionId).orElse(UNKNOWN_VALUE))
				.jobTriggerId(ctx.jobTrigger().map(JobTriggerDto::id).orElse(UNKNOWN_VALUE))
				.event(ctx.event())
				.backlog(backlog)
				.backlogSize(backlog.size())
				.graylogUrl(isNullOrEmpty(config.graylogUrl()) ? UNKNOWN_VALUE : config.graylogUrl())
				.streams(streams)
				.build();

		return objectMapper.convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
	}

	private Map<String, Object> getBacklogItemModel(EventNotificationContext ctx, SlackEventNotificationConfig config, MessageSummary backlogItem) {
		Optional<EventDefinitionDto> definitionDto = ctx.eventDefinition();

		List<StreamModelData> streams = streamService.loadByIds(ctx.event().sourceStreams())
				.stream()
				.map(stream -> buildStreamWithUrl(stream, ctx, config))
				.collect(Collectors.toList());

		BacklogItemModelData modelData = BacklogItemModelData.builder()
				.eventDefinition(definitionDto)
				.eventDefinitionId(definitionDto.map(EventDefinitionDto::id).orElse(UNKNOWN_VALUE))
				.eventDefinitionType(definitionDto.map(d -> d.config().type()).orElse(UNKNOWN_VALUE))
				.eventDefinitionTitle(definitionDto.map(EventDefinitionDto::title).orElse(UNKNOWN_VALUE))
				.eventDefinitionDescription(definitionDto.map(EventDefinitionDto::description).orElse(UNKNOWN_VALUE))
				.jobDefinitionId(ctx.jobTrigger().map(JobTriggerDto::jobDefinitionId).orElse(UNKNOWN_VALUE))
				.jobTriggerId(ctx.jobTrigger().map(JobTriggerDto::id).orElse(UNKNOWN_VALUE))
				.event(ctx.event())
				.backlogItem(backlogItem)
				.graylogUrl(isNullOrEmpty(config.graylogUrl()) ? UNKNOWN_VALUE : config.graylogUrl())
				.streams(streams)
				.build();

		return objectMapper.convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
	}

	private StreamModelData buildStreamWithUrl(Stream stream, EventNotificationContext ctx, SlackEventNotificationConfig config) {
		String graylogUrl = config.graylogUrl();
		String streamUrl = null;
		if(!isNullOrEmpty(graylogUrl)) {
			streamUrl = StringUtils.appendIfMissing(graylogUrl, "/") + "streams/" + stream.getId() + "/search";

			if(ctx.eventDefinition().isPresent()) {
				EventDefinitionDto eventDefinitionDto = ctx.eventDefinition().get();
				if(eventDefinitionDto.config() instanceof AggregationEventProcessorConfig) {
					String query = ((AggregationEventProcessorConfig) eventDefinitionDto.config()).query();
					streamUrl += "?q=" + query;
				}
			}
		}

		return StreamModelData.builder()
				.id(stream.getId())
				.title(stream.getTitle())
				.description(stream.getDescription())
				.url(Optional.ofNullable(streamUrl).orElse(UNKNOWN_VALUE))
				.build();
	}
}
