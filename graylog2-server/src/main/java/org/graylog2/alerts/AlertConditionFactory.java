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

import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class AlertConditionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AlertConditionFactory.class);
    private final Map<String, AlertCondition.Factory> alertConditionMap;

    @Inject
    public AlertConditionFactory(Map<String, AlertCondition.Factory> alertConditionMap) {
        this.alertConditionMap = alertConditionMap;
    }

    public AlertCondition createAlertCondition(String type,
                                               Stream stream,
                                               String id,
                                               DateTime createdAt,
                                               String creatorId,
                                               Map<String, Object> parameters,
                                               String title) throws ConfigurationException {

        final AlertCondition.Factory factory = this.alertConditionMap.get(type);
        checkArgument(factory != null, "Unknown alert condition type: " + type);

        /*
         * Ensure the given parameters fulfill the requested configuration preconditions.
         * Here we strictly use the Configuration object to verify the configuration and don't pass it down to
         * the factory. The reason for this is that Configuration only support int values, but at least an
         * alert condition expects a double.
         */
        try {
            final ConfigurationRequest requestedConfiguration = factory.config().getRequestedConfiguration();
            final Configuration configuration = new Configuration(parameters);
            requestedConfiguration.check(configuration);
        } catch (ConfigurationException e) {
            final String conditionTitle = isNullOrEmpty(title) ? "" : "'" + title + "' ";
            LOG.error("Could not load alert condition " + conditionTitle + "<" + id + ">, invalid configuration detected.");
            throw e;
        }

        return factory.create(stream, id, createdAt, creatorId, parameters, title);
    }
}
