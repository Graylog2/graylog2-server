/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alerts;

import com.floreysoft.jmte.Engine;
import org.graylog2.Configuration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.users.UserService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class FormattedEmailAlertSender extends StaticEmailAlertSender implements AlertSender {
    private org.graylog2.plugin.configuration.Configuration pluginConfig;
    public static final String bodyTemplate = "##########\n" +
            "Date: ${check_result.triggeredAt}\n" +
            "Stream ID: ${stream.id}\n" +
            "Stream title: ${stream.title}\n" +
            "Stream URL: ${stream_url}\n" +
            "\n" +
            "Triggered condition: ${check_result.triggeredCondition}\n" +
            "##########\n\n" +
            "Last messages accounting for this alert:\n" +
            "${if backlog_size > 0}" +
            "${foreach backlog message}\n" +
            "${message}\n" +
            "${end}\n" +
            "${else}<No backlog.>${end}\n" +
            "\n";

    @Inject
    public FormattedEmailAlertSender(Configuration configuration, StreamRuleService streamRuleService, UserService userService) {
        super(configuration, streamRuleService, userService);
    }

    @Override
    public void initialize(org.graylog2.plugin.configuration.Configuration configuration) {
        this.pluginConfig = configuration;
    }

    @Override
    protected String buildSubject(Stream stream, AlertCondition.CheckResult checkResult, Configuration config, List<Message> backlog) {
        final String template;
        if (pluginConfig == null || pluginConfig.getString("subject") == null)
            template = "Graylog2 alert for stream: ${stream.title}";
        else
            template = pluginConfig.getString("subject");
        Map<String, Object> model = getModel(stream, checkResult, backlog);
        Engine engine = new Engine();
        String transformed = engine.transform(template, model);

        return transformed;
    }

    @Override
    protected String buildBody(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        final String template;
        if (pluginConfig == null || pluginConfig.getString("body") == null)
            template = bodyTemplate;
        else
            template = pluginConfig.getString("body");
        Map<String, Object> model = getModel(stream, checkResult, backlog);
        Engine engine = new Engine();
        String transformed = engine.transform(template, model);

        return transformed;
    }

    private Map<String, Object> getModel(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("stream", stream);
        model.put("check_result", checkResult);
        model.put("stream_url", buildStreamDetailsURL(configuration.getEmailTransportWebInterfaceUrl(),
                checkResult, stream));
        if (backlog != null) {
            model.put("backlog", backlog);
            model.put("backlog_size", backlog.size());
        } else {
            model.put("backlog", new ArrayList<Message>());
            model.put("backlog_size", 0);
        }
        return model;
    }
}
