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
package org.graylog.plugins.pipelineprocessor.functions.alerts;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertNotificationsSender;
import org.graylog2.alerts.AlertService;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.DefaultStream;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class TriggerAlert extends AbstractFunction<List<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(TriggerAlert.class);
    @SuppressWarnings("unchecked")
    private static final Class<List<String>> RETURN_TYPE = (Class<List<String>>) new TypeToken<List<String>>() {
    }.getRawType();

    public static final String NAME = "trigger_alert";

    private final AlertService alertService;
    private final AlertNotificationsSender alertNotificationsSender;
    private final StreamCacheService streamCacheService;
    private final Provider<Stream> defaultStreamProvider;

    private final ParameterDescriptor<Message, Message> messageParam;
    private final ParameterDescriptor<String, String> titleParam;
    private final ParameterDescriptor<String, String> descriptionParam;
    private final ParameterDescriptor<String, String> streamIdParam;
    private final ParameterDescriptor<String, String> streamNameParam;
    private final ParameterDescriptor<Boolean, Boolean> notifyParam;
    private final ParameterDescriptor<Boolean, Boolean> resolveParam;

    @Inject
    public TriggerAlert(AlertService alertService,
                        AlertNotificationsSender alertNotificationsSender,
                        StreamCacheService streamCacheService,
                        @DefaultStream Provider<Stream> defaultStreamProvider) {
        this.alertService = alertService;
        this.alertNotificationsSender = alertNotificationsSender;
        this.streamCacheService = streamCacheService;
        this.defaultStreamProvider = defaultStreamProvider;

        messageParam = type("message", Message.class).optional().description("The message to drop, defaults to '$message'").build();
        titleParam = string("title").optional().description("Title of the alert condition").build();
        descriptionParam = string("description").optional().description("Description of the alert condition").build();
        streamIdParam = string("stream_id").optional().description("The ID of the stream").build();
        streamNameParam = string("stream_name").optional().description("The name of the stream, must match exactly").build();
        notifyParam = bool("notify").description("Send alert notifications, defaults to 'true'").optional().build();
        resolveParam = bool("resolve").description("Resolve alert immediately, defaults to 'true'").optional().build();
    }

    @Override
    public List<String> evaluate(FunctionArgs args, EvaluationContext context) {
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        final String title = titleParam.optional(args, context).orElse("Manual alert for message " + message.getId());
        final String description = descriptionParam.optional(args, context).orElse("Manual alert for message " + message.getId());
        final boolean notify = notifyParam.optional(args, context).orElse(true);
        final boolean resolve = resolveParam.optional(args, context).orElse(true);

        final Optional<String> streamId = streamIdParam.optional(args, context);
        final Collection<Stream> streams;
        if (streamId.isPresent()) {
            streams = streamId
                    .map(streamCacheService::getById)
                    .map(ImmutableList::of)
                    .orElse(ImmutableList.of(defaultStreamProvider.get()));
        } else {
            streams = streamNameParam.optional(args, context)
                    .map(streamCacheService::getByName)
                    .orElse(ImmutableList.of(defaultStreamProvider.get()));
        }

        final MessageSummary messageSummary = createMessageSummary(message);
        final ImmutableList.Builder<String> alertIds = ImmutableList.builder();
        for (Stream stream : streams) {
            if (stream.isPaused()) {
                continue;
            }

            final AlertCondition alertCondition = createAlertCondition(stream, title, description, messageSummary);
            final AlertCondition.CheckResult checkResult = alertCondition.runCheck();
            final Alert alert = alertService.factory(checkResult);
            try {
                alertService.save(alert);
            } catch (ValidationException e) {
                LOG.error("Failed to save alert {}", alert, e);
                return null;
            }

            alertIds.add(alert.getId());

            if (resolve) {
                final Alert resolvedAlert = alertService.resolveAlert(alert);
                LOG.debug("Resolved alert {}", resolvedAlert);
            }

            if (notify) {
                LOG.debug("Sending alert notifications for alert {} on stream {}", alert, stream);
                alertNotificationsSender.send(checkResult, stream, alert, alertCondition);
            }
        }

        return alertIds.build();
    }

    private AlertCondition createAlertCondition(Stream stream, String title, String description, MessageSummary messageSummary) {
        return new DummyAlertCondition(
                stream,
                messageSummary.getId(),
                description,
                ImmutableList.of(messageSummary),
                Tools.nowUTC(),
                "$system", // TODO: Better default user name?
                Collections.emptyMap(),
                title);
    }

    private MessageSummary createMessageSummary(Message message) {
        return new MessageSummary("", message);
    }

    @Override
    public FunctionDescriptor<List<String>> descriptor() {
        return FunctionDescriptor.<List<String>>builder()
                .name(NAME)
                .returnType(RETURN_TYPE)
                .params(messageParam,
                        titleParam,
                        descriptionParam,
                        streamIdParam,
                        streamNameParam,
                        notifyParam,
                        resolveParam)
                .description("Trigger an alert for the current message")
                .build();
    }
}
