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
package org.graylog2.alerts;

import com.floreysoft.jmte.Engine;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.streams.StreamRuleService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class FormattedEmailAlertSender extends StaticEmailAlertSender implements AlertSender {
    public static final String bodyTemplate = "##########\n" +
            "Alert Description: ${check_result.resultDescription}\n" +
            "Date: ${check_result.triggeredAt}\n" +
            "Stream ID: ${stream.id}\n" +
            "Stream title: ${stream.title}\n" +
            "Stream description: ${stream.description}\n" +
            "Alert Condition Title: ${alertCondition.title}\n" +
            "${if stream_url}Stream URL: ${stream_url}${end}\n" +
            "\n" +
            "Triggered condition: ${check_result.triggeredCondition}\n" +
            "##########\n\n" +
            "${if backlog}" +
            "Last messages accounting for this alert:\n" +
            "${foreach backlog message}" +
            "${message}\n\n" +
            "${end}" +
            "${else}" +
            "<No backlog>\n" +
            "${end}" +
            "\n";

    private final Engine engine = new Engine();
    private Configuration pluginConfig;

    @Inject
    public FormattedEmailAlertSender(EmailConfiguration configuration,
                                     StreamRuleService streamRuleService,
                                     NotificationService notificationService,
                                     NodeId nodeId) {
        super(configuration, streamRuleService, notificationService, nodeId);
    }

    @Override
    public void initialize(Configuration configuration) {
        this.pluginConfig = configuration;
        super.initialize(configuration);
    }

    @Override
    protected String buildSubject(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        final String template;
        if (pluginConfig == null || pluginConfig.getString("subject") == null) {
            template = "Graylog alert for stream: ${stream.title}: ${check_result.resultDescription}";
        } else {
            template = pluginConfig.getString("subject");
        }

        Map<String, Object> model = getModel(stream, checkResult, backlog);
        Engine engine = new Engine();

        return engine.transform(template, model);
    }

    @Override
    protected String buildBody(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        final String template;
        if (pluginConfig == null || pluginConfig.getString("body") == null) {
            template = bodyTemplate;
        } else {
            template = pluginConfig.getString("body");
        }
        Map<String, Object> model = getModel(stream, checkResult, backlog);

        return engine.transform(template, model);
    }

    private Map<String, Object> getModel(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        Map<String, Object> model = new HashMap<>();
        model.put("stream", stream);
        model.put("check_result", checkResult);
        model.put("stream_url", buildStreamDetailsURL(configuration.getWebInterfaceUri(), checkResult, stream));
        model.put("alertCondition", checkResult.getTriggeredCondition());

        final List<Message> messages = firstNonNull(backlog, Collections.<Message>emptyList());
        model.put("backlog", messages);
        model.put("backlog_size", messages.size());

        return model;
    }
}
