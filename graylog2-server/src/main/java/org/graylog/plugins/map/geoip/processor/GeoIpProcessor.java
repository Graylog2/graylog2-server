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
package org.graylog.plugins.map.geoip.processor;

import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog.plugins.map.config.S3GeoIpFileService;
import org.graylog.plugins.map.geoip.GeoIpDbFileChangedEvent;
import org.graylog.plugins.map.geoip.GeoIpResolverEngine;
import org.graylog.plugins.map.geoip.GeoIpVendorResolverService;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GeoIpProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(GeoIpProcessor.class);

    public static class Descriptor implements MessageProcessor.Descriptor {
        @Override
        public String name() {
            return "GeoIP Resolver";
        }

        @Override
        public String className() {
            return GeoIpProcessor.class.getCanonicalName();
        }
    }

    private final ClusterConfigService clusterConfigService;
    private final ScheduledExecutorService scheduler;
    private final MetricRegistry metricRegistry;
    private final GeoIpVendorResolverService geoIpVendorResolverService;
    private final ServerStatus serverStatus;
    private final S3GeoIpFileService s3GeoIpFileService;

    private final AtomicReference<GeoIpResolverEngine> filterEngine = new AtomicReference<>(null);

    @Inject
    public GeoIpProcessor(ClusterConfigService clusterConfigService,
                          @Named("daemonScheduler") ScheduledExecutorService scheduler,
                          EventBus eventBus,
                          MetricRegistry metricRegistry,
                          GeoIpVendorResolverService geoIpVendorResolverService,
                          ServerStatus serverStatus,
                          S3GeoIpFileService s3GeoIpFileService) {
        this.clusterConfigService = clusterConfigService;
        this.scheduler = scheduler;
        this.metricRegistry = metricRegistry;
        this.geoIpVendorResolverService = geoIpVendorResolverService;
        this.serverStatus = serverStatus;
        this.s3GeoIpFileService = s3GeoIpFileService;

        eventBus.register(this);
    }

    @Override
    public Messages process(Messages messages) {

        if (filterEngine.get() == null) {
            try {
                serverStatus.awaitRunning();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("The GeoIpProcessor was interrupted while waiting for the Server to start up.");
                return messages;
            }
            reload();
        }

        for (Message message : messages) {
            filterEngine.get().filter(message);
        }

        return messages;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void updateConfig(ClusterConfigChangedEvent event) {
        if (!GeoIpResolverConfig.class.getCanonicalName().equals(event.type())) {
            return;
        }

        scheduler.schedule(this::reload, 0, TimeUnit.SECONDS);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onDatabaseFileChangedEvent(GeoIpDbFileChangedEvent event) {

        scheduler.schedule(this::reload, 0, TimeUnit.SECONDS);
    }

    private void reload() {
        final GeoIpResolverConfig newConfig = clusterConfigService.getOrDefault(GeoIpResolverConfig.class,
                GeoIpResolverConfig.defaultConfig());

        LOG.debug("Updating GeoIP resolver engine - {}", newConfig);
        filterEngine.set(new GeoIpResolverEngine(geoIpVendorResolverService, newConfig, s3GeoIpFileService, metricRegistry));
    }
}
