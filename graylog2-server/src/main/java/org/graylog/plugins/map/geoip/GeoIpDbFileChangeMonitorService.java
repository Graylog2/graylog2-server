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
package org.graylog.plugins.map.geoip;

import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog.plugins.map.config.DatabaseType;
import org.graylog.plugins.map.config.DatabaseVendorType;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog.plugins.map.config.S3DownloadException;
import org.graylog.plugins.map.config.S3GeoIpFileService;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.utilities.FileInfo;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.graylog2.rest.resources.system.GeoIpResolverConfigValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A service to check whether the ASN and City MaxMind/IPInfo database files have changed, as well as whether the configuration has changed.
 *
 * <p>
 * If the database files have changed, a {@link GeoIpDbFileChangedEvent} is posted, to which {@link org.graylog.plugins.map.geoip.processor.GeoIpProcessor} subscribes and reloads the {@link GeoIpResolverEngine}.
 * </p>
 *
 * <p>
 * This service also subscribes to {@link ClusterConfigChangedEvent} to update the database files to be monitored, as well as to update the scheduled task ({@link #refreshDatabases()}) which checks for file changes.
 * </p>
 */

@Singleton
public final class GeoIpDbFileChangeMonitorService extends AbstractIdleService {

    private static final Logger LOG = LoggerFactory.getLogger(GeoIpDbFileChangeMonitorService.class.getSimpleName());

    private ScheduledFuture<?> refreshTask;

    private final ScheduledExecutorService scheduler;
    private final GeoIpResolverConfigValidator geoIpResolverConfigValidator;
    private final S3GeoIpFileService s3GeoIpFileService;
    private final NotificationService notificationService;

    private final ClusterConfigService clusterConfigService;
    private final EventBus eventBus;
    private Duration dbRefreshInterval = Duration.ZERO;
    private FileInfo cityDbFileInfo = FileInfo.empty();
    private FileInfo asnDbFileInfo = FileInfo.empty();
    private GeoIpResolverConfig config;

    @Inject
    public GeoIpDbFileChangeMonitorService(@Named("daemonScheduler") ScheduledExecutorService scheduler,
                                           EventBus eventBus,
                                           ClusterConfigService clusterConfigService,
                                           GeoIpVendorResolverService geoIpVendorResolverService,
                                           S3GeoIpFileService s3GeoIpFileService,
                                           NotificationService notificationService) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.eventBus = Objects.requireNonNull(eventBus);
        this.s3GeoIpFileService = Objects.requireNonNull(s3GeoIpFileService);
        this.clusterConfigService = Objects.requireNonNull(clusterConfigService);
        this.geoIpResolverConfigValidator = new GeoIpResolverConfigValidator(geoIpVendorResolverService, s3GeoIpFileService, clusterConfigService);
        this.notificationService = notificationService;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onClusterConfigChanged(ClusterConfigChangedEvent event) {
        if (GeoIpResolverConfig.class.getCanonicalName().equals(event.type())) {
            scheduler.schedule(this::updateConfiguration, 0, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void startUp() throws Exception {

        eventBus.register(this);
        updateConfiguration();
    }

    @Override
    protected void shutDown() throws Exception {

        eventBus.unregister(this);
    }

    private void refreshDatabases() {

        LOG.debug("Starting GeoIP database refresh");

        Map<DatabaseType, FileInfo.Change> changes = checkForChanges();

        if (changes.isEmpty()) {
            LOG.debug("GeoIP Database files have not changed--will not refresh");
        } else {
            GeoIpDbFileChangedEvent event = GeoIpDbFileChangedEvent.create();
            eventBus.post(event);
        }
    }

    private Map<DatabaseType, FileInfo.Change> checkForChanges() {

        if (config == null) {
            config = getCurrentConfig();
        }
        Map<DatabaseType, FileInfo.Change> changes = new EnumMap<>(DatabaseType.class);
        // If using S3 for database file storage, check to see if the files are new
        if (config.useS3() && s3GeoIpFileService.fileRefreshRequired(config)) {
            try {
                // Download the new files to a temporary location to be validated
                LOG.debug("Pulling DB files from S3");
                s3GeoIpFileService.downloadFilesToTempLocation(config);
                GeoIpResolverConfig tempConfig = config.toBuilder()
                        .cityDbPath(s3GeoIpFileService.getTempCityFile())
                        .asnDbPath(config.asnDbPath().isEmpty() ? "" : s3GeoIpFileService.getTempAsnFile())
                        .build();
                Timer timer = new Timer(new UniformReservoir());
                geoIpResolverConfigValidator.validateGeoIpLocationResolver(tempConfig, timer);
                geoIpResolverConfigValidator.validateGeoIpAsnResolver(tempConfig, timer);

                // Now that the new files have passed validation, move them to the active file location
                s3GeoIpFileService.moveTempFilesToActive();
                LOG.debug("Pulled new files from S3");
            } catch (IllegalArgumentException | IllegalStateException validationError) {
                String message = "Geo Processor DB files from S3 failed validation. Upload valid files to S3. Leaving old files in place on disk.";
                sendFailedSyncNotification(message);
                LOG.error(message);
                s3GeoIpFileService.cleanupTempFiles();
                return changes;
            } catch (S3DownloadException | IOException e) {
                String message = "Failed to download Geo Processor DB files from S3. Unable to refresh. Leaving old files in place on disk.";
                sendFailedSyncNotification(message);
                LOG.error(message);
                return changes;
            }
        }
        FileInfo.Change cityDbChange = cityDbFileInfo.checkForChange();
        FileInfo.Change asnDbChange = asnDbFileInfo.checkForChange();

        DatabaseVendorType vendorType = config.databaseVendorType();

        if (cityDbChange.isChanged()) {
            changes.put(vendorType.getCityDbType(), cityDbChange);
            cityDbFileInfo = cityDbChange.fileInfo();
        }

        if (asnDbChange.isChanged()) {
            changes.put(vendorType.getAsnDbType(), asnDbChange);
            asnDbFileInfo = asnDbChange.fileInfo();
        }
        return changes;
    }

    private void updateConfiguration() {

        try {
            config = getCurrentConfig();

            if (config.enabled()) {
                reScheduleRefreshIfNeeded();
                String asnFile = config.asnDbPath();
                String cityFile = config.cityDbPath();
                if (config.useS3()) {
                    cityFile = s3GeoIpFileService.getActiveCityFile();
                    asnFile = s3GeoIpFileService.getActiveAsnFile();
                    if (s3GeoIpFileService.fileRefreshRequired(config)) {
                        try {
                            // This should only be true in multi-Graylog-node environments if the processor config is
                            // changed on a different Graylog node. The files may not yet exist on-disk on this node and
                            // will need to be downloaded first. The files have already been validated on the original node.
                            if (s3GeoIpFileService.fileRefreshRequired(config)) {
                                s3GeoIpFileService.downloadFilesToTempLocation(config);
                                s3GeoIpFileService.moveTempFilesToActive();
                            }
                        } catch (S3DownloadException | IOException e) {
                            String commonMessage = "Failed to pull new Geo-Location Processor database files from S3.";
                            sendFailedSyncNotification(commonMessage + " Geo-Location Processor may not be functional on all nodes.");
                            LOG.error("{} Geo-Location Processor will not be functional on this node.", commonMessage);
                            return;
                        }
                    }
                }
                geoIpResolverConfigValidator.validate(config);
                this.cityDbFileInfo = getDbFileInfo(cityFile);
                this.asnDbFileInfo = getDbFileInfo(asnFile);
            } else {
                LOG.debug("GeoIP Processor is disabled.  Will not schedule GeoIP database file change monitor");
                cancelScheduledRefreshTask();

                // Set interval to ZERO to allow rescheduling when enabled again, even if interval is not changed.
                dbRefreshInterval = Duration.ZERO;
            }

        } catch (ConfigValidationException | IllegalArgumentException | IllegalStateException e) {
            LOG.error("Error validating GeoIP Database files. {}", e.getMessage(), e);
        }
    }

    private void cancelScheduledRefreshTask() {
        if (refreshTask != null) {
            boolean canceled = refreshTask.cancel(true);
            if (canceled) {
                LOG.debug("The GeoIP database file change monitor was running.  It has been cancelled");
                refreshTask = null;
            } else {
                LOG.warn("The GeoIP database file change monitor was running and failed to stop it");
            }
        }
    }

    private void reScheduleRefreshIfNeeded() {
        if (!dbRefreshInterval.equals(config.refreshIntervalAsDuration())) {
            boolean reschedule = refreshTask == null || refreshTask.cancel(true);
            if (reschedule) {
                this.dbRefreshInterval = config.refreshIntervalAsDuration();
                scheduleDbRefresh();
            } else {
                LOG.warn("Failed to Cancel existing GeoIp Database Refresh Task.  Will not update refresh interval.");
            }
        }
    }

    private FileInfo getDbFileInfo(String path) {
        try {
            return FileInfo.forPath(Paths.get(path));
        } catch (Exception e) {
            return FileInfo.empty();
        }
    }

    private void scheduleDbRefresh() {

        try {
            long millis = dbRefreshInterval.toMillis();
            refreshTask = scheduler.scheduleAtFixedRate(this::refreshDatabases, millis, millis, TimeUnit.MILLISECONDS);
            LOG.debug("Scheduled GeoIP database refresh every '{}' Milliseconds", millis);
        } catch (Exception e) {
            LOG.error("Error scheduling GeoIP database refresh job. {}", e.getMessage(), e);
        }
    }

    private GeoIpResolverConfig getCurrentConfig() {
        return clusterConfigService.getOrDefault(GeoIpResolverConfig.class,
                GeoIpResolverConfig.defaultConfig());
    }

    private void sendFailedSyncNotification(String description) {
        final Notification notification = notificationService.buildNow()
                .addType(Notification.Type.GENERIC)
                .addSeverity(Notification.Severity.NORMAL)
                .addDetail("title", "Geo-Location Processor S3 Sync Failure")
                .addDetail("description", description);
        notificationService.publishIfFirst(notification);
    }

}
