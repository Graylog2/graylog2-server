/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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

        final String conditionTitle = isNullOrEmpty(title) ? "" : "'" + title + "' ";
        final AlertCondition.Factory factory = this.alertConditionMap.get(type);
        checkArgument(factory != null, "Unknown alert condition type <%s> for alert condition %s<%s> on stream \"%s\" <%s>",
                type, conditionTitle, id, stream.getTitle(), stream.getId());

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
            LOG.error("Could not load alert condition {}<{}> on stream \"{}\" <{}>, invalid configuration detected.", conditionTitle, id, stream.getTitle(), stream.getId());
            throw e;
        }

        return factory.create(stream, id, createdAt, creatorId, parameters, title);
    }
}
