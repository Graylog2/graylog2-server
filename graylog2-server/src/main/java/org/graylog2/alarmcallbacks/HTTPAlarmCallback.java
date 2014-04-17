/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.alarmcallbacks;

import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.streams.Stream;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class HTTPAlarmCallback implements AlarmCallback {
    private Configuration configuration;

    @Override
    public void initialize(Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;
    }

    @Override
    public void call(Stream stream, AlertCondition alertCondition, AlertCondition.CheckResult result) throws AlarmCallbackException {
        System.out.println("Calling " + configuration.getString("url"));
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField("url",
                "URL",
                "https://example.org/alerts",
                "The URL to POST to when an alert is triggered",
                ConfigurationField.Optional.NOT_OPTIONAL));

        return configurationRequest;
    }

    @Override
    public String getName() {
        return "HTTP Alarm Callback";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        if (configuration.getString("url") == null || configuration.getString("url").isEmpty())
            throw new ConfigurationException("URL parameter is missing!");
    }
}
