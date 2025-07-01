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
package org.graylog.integrations.dbconnector;

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.graylog.integrations.dbconnector.external.DBConnectorClient;
import org.graylog.integrations.dbconnector.external.DBConnectorClientFactory;
import org.graylog.integrations.dbconnector.external.DBConnectorTransferObject;
import org.graylog2.inputs.persistence.InputStatusService;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_TYPE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_POLLING_INTERVAL;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_POLLING_TIME_UNIT;

public class DBConnectorTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnectorTransport.class);

    private final LocalMetricRegistry localRegistry;
    private final ScheduledExecutorService executorService;
    private final InputStatusService inputStatusService;
    private final DBConnectorClientFactory DBConnectorClientFactory;
    private ScheduledFuture runningTask = null;

    @AssistedInject
    public DBConnectorTransport(@Assisted Configuration configuration,
                                EventBus eventBus,
                                LocalMetricRegistry localRegistry,
                                InputStatusService inputStatusService,
                                DBConnectorClientFactory DBConnectorClientFactory,
                                @Named("daemonScheduler") ScheduledExecutorService executorService) {
        super(eventBus, configuration);
        this.localRegistry = localRegistry;
        this.inputStatusService = inputStatusService;
        this.DBConnectorClientFactory = DBConnectorClientFactory;
        this.executorService = executorService;
    }

    @Override
    protected void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        LOG.debug("Launching DBConnectorTransport");
        final String dbType = input.getConfiguration().getString(CK_DATABASE_TYPE);
        long pollingInterval = input.getConfiguration().getInt(CK_POLLING_INTERVAL);
        final TimeUnit pollingTimeUnit = TimeUnit.valueOf(input.getConfiguration().getString(CK_POLLING_TIME_UNIT));

        DBConnectorClient dbConnectorClient = null;

        try {
            dbConnectorClient = DBConnectorClientFactory.getClient(dbType);
        } catch (MalformedURLException | SQLException e) {
            LOG.error("Database Client count not be acquired. [{}]", e.toString());
            throw new MisfireException("Unable to create Database client", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DBConnectorTransferObject.Builder dtoBuilder = DBConnectorTransferObject.builder();
        dtoBuilder.databaseType(dbType);
        LOG.debug("Constructing poller task");
        DBConnectorPollerTask pollerTask = new DBConnectorPollerTask((DBConnectorInput) input,
                inputStatusService,
                dbConnectorClient,
                this,
                dtoBuilder, inputFailureRecorder);

        LOG.debug("Submitting poller task to executor");
        runningTask = executorService.scheduleWithFixedDelay(pollerTask, 0L, pollingInterval, pollingTimeUnit);
    }

    @Override
    protected void doStop() {
        LOG.debug("Stopping DBConnectorTransport");
        if (null != runningTask) {
            LOG.debug("Cancelling scheduled task");
            runningTask.cancel(true);
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<DBConnectorTransport> {
        @Override
        DBConnectorTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport2.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return super.getRequestedConfiguration();
        }
    }
}
