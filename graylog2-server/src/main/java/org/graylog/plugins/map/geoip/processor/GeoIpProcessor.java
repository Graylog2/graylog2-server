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
import org.graylog.plugins.map.geoip.GeoIpResolverEngine;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
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

    private final AtomicReference<GeoIpResolverConfig> config;
    private final AtomicReference<GeoIpResolverEngine> filterEngine;

    @Inject
    public GeoIpProcessor(ClusterConfigService clusterConfigService,
                          @Named("daemonScheduler") ScheduledExecutorService scheduler,
                          EventBus eventBus,
                          MetricRegistry metricRegistry) {
        this.clusterConfigService = clusterConfigService;
        this.scheduler = scheduler;
        this.metricRegistry = metricRegistry;
        final GeoIpResolverConfig config = clusterConfigService.getOrDefault(GeoIpResolverConfig.class,
                GeoIpResolverConfig.defaultConfig());

        this.config = new AtomicReference<>(config);
        this.filterEngine = new AtomicReference<>(new GeoIpResolverEngine(config, metricRegistry));

        eventBus.register(this);
    }

    @Override
    public Messages process(Messages messages) {
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

        scheduler.schedule((Runnable) this::reload, 0, TimeUnit.SECONDS);
    }

    private void reload() {
        final GeoIpResolverConfig newConfig = clusterConfigService.getOrDefault(GeoIpResolverConfig.class,
                GeoIpResolverConfig.defaultConfig());

        LOG.info("Updating GeoIP resolver engine - {}", newConfig);
        config.set(newConfig);
        filterEngine.set(new GeoIpResolverEngine(newConfig, metricRegistry));
    }
}
