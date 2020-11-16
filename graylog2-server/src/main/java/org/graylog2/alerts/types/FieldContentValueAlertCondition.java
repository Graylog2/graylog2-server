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
package org.graylog2.alerts.types;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.Configuration;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FieldContentValueAlertCondition extends AbstractAlertCondition {
    private static final Logger LOG = LoggerFactory.getLogger(FieldContentValueAlertCondition.class);

    private final Searches searches;
    private final Configuration configuration;
    private final String field;
    private final String value;
    private final String query;

    public interface Factory extends AlertCondition.Factory {
        @Override
        FieldContentValueAlertCondition create(Stream stream,
                                               @Assisted("id") String id,
                                               DateTime createdAt,
                                               @Assisted("userid") String creatorUserId,
                                               Map<String, Object> parameters,
                                               @Assisted("title") @Nullable String title);

        @Override
        Config config();

        @Override
        Descriptor descriptor();
    }

    public static class Config implements AlertCondition.Config {
        public Config() {
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(
                    new TextField("field", "Field", "", "Field name that should be checked", ConfigurationField.Optional.NOT_OPTIONAL),
                    new TextField("value", "Value", "", "Value that the field should be checked against", ConfigurationField.Optional.NOT_OPTIONAL)
            );
            configurationRequest.addFields(AbstractAlertCondition.getDefaultConfigurationFields());

            return configurationRequest;
        }
    }

    public static class Descriptor extends AlertCondition.Descriptor {
        public Descriptor() {
            super(
                "Field Content Alert Condition",
                "https://www.graylog.org/",
                "This condition is triggered when the content of messages is equal to a defined value."
            );
        }
    }

    @AssistedInject
    public FieldContentValueAlertCondition(Searches searches,
                                           Configuration configuration,
                                           @Assisted Stream stream,
                                           @Nullable @Assisted("id") String id,
                                           @Assisted DateTime createdAt,
                                           @Assisted("userid") String creatorUserId,
                                           @Assisted Map<String, Object> parameters,
                                           @Assisted("title") @Nullable String title) {
        super(stream, id, Type.FIELD_CONTENT_VALUE.toString(), createdAt, creatorUserId, parameters, title);
        this.searches = searches;
        this.configuration = configuration;
        this.field = (String) parameters.get("field");
        this.value = (String) parameters.get("value");
        this.query = (String) parameters.getOrDefault(CK_QUERY, CK_QUERY_DEFAULT_VALUE);
    }

    @Override
    public CheckResult runCheck() {
        String filter = buildQueryFilter(stream.getId(), query);
        String query = field + ":\"" + value + "\"";
        Integer backlogSize = getBacklog();
        boolean backlogEnabled = false;
        int searchLimit = 1;

        if(backlogSize != null && backlogSize > 0) {
            backlogEnabled = true;
            searchLimit = backlogSize;
        }

        try {
            SearchResult result = searches.search(
                query,
                filter,
                RelativeRange.create(configuration.getAlertCheckInterval()),
                searchLimit,
                0,
                new Sorting(Message.FIELD_TIMESTAMP, Sorting.Direction.DESC)
            );

            final List<MessageSummary> summaries;
            if (backlogEnabled) {
                summaries = Lists.newArrayListWithCapacity(result.getResults().size());
                for (ResultMessage resultMessage : result.getResults()) {
                    final Message msg = resultMessage.getMessage();
                    summaries.add(new MessageSummary(resultMessage.getIndex(), msg));
                }
            } else {
                summaries = Collections.emptyList();
            }

            final long count = result.getTotalResults();

            final String resultDescription = "Stream received messages matching <" + query + "> "
                + "(Current grace time: " + grace + " minutes)";

            if (count > 0) {
                LOG.debug("Alert check <{}> found [{}] messages.", id, count);
                return new CheckResult(true, this, resultDescription, Tools.nowUTC(), summaries);
            } else {
                LOG.debug("Alert check <{}> returned no results.", id);
                return new NegativeCheckResult();
            }
        } catch (InvalidRangeParametersException e) {
            // cannot happen lol
            LOG.error("Invalid timerange.", e);
            return null;
        }
    }

    @Override
    public String getDescription() {
        return "field: " + field
                + ", value: " + value
                + ", grace: " + grace
                + ", repeat notifications: " + repeatNotifications;
    }
}
