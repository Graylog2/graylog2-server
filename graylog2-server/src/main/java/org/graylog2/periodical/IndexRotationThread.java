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
package org.graylog2.periodical;

import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;

public class IndexRotationThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRotationThread.class);

    private NotificationService notificationService;
    private final Deflector deflector;
    private final Cluster cluster;
    private final ActivityWriter activityWriter;
    private final Indices indices;
    private final ClusterConfigService clusterConfigService;
    private final Map<String, Provider<RotationStrategy>> rotationStrategyMap;

    @Inject
    public IndexRotationThread(NotificationService notificationService,
                               Indices indices,
                               Deflector deflector,
                               Cluster cluster,
                               ActivityWriter activityWriter,
                               ClusterConfigService clusterConfigService,
                               Map<String, Provider<RotationStrategy>> rotationStrategyMap) {
        this.notificationService = notificationService;
        this.deflector = deflector;
        this.cluster = cluster;
        this.activityWriter = activityWriter;
        this.indices = indices;
        this.clusterConfigService = clusterConfigService;
        this.rotationStrategyMap = rotationStrategyMap;
    }

    @Override
    public void doRun() {
        // Point deflector to a new index if required.
        if (cluster.isConnected()) {
            try {
                checkAndRepair();
                checkForRotation();
            } catch (Exception e) {
                LOG.error("Couldn't point deflector to a new index", e);
            }
        } else {
            LOG.debug("Elasticsearch cluster isn't healthy. Skipping index rotation.");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected void checkForRotation() {
        final Optional<IndexManagementConfig> config = clusterConfigService.get(IndexManagementConfig.class);

        if (!config.isPresent()) {
            LOG.warn("No index management configuration found, not running index rotation!");
            return;
        }

        final Provider<RotationStrategy> rotationStrategyProvider = rotationStrategyMap.get(config.get().rotationStrategy());

        if (rotationStrategyProvider == null) {
            LOG.warn("Rotation strategy \"{}\" not found, not running index rotation!", config.get().rotationStrategy());
            return;
        }

        final RotationStrategy rotationStrategy = rotationStrategyProvider.get();

        if (rotationStrategy == null) {
            LOG.warn("No rotation strategy found, not running index rotation!");
            return;
        }

        rotationStrategy.rotate();
    }

    protected void checkAndRepair() {
        if (!deflector.isUp()) {
            if (indices.exists(deflector.getName())) {
                // Publish a notification if there is an *index* called graylog2_deflector
                Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.DEFLECTOR_EXISTS_AS_INDEX)
                        .addSeverity(Notification.Severity.URGENT);
                final boolean published = notificationService.publishIfFirst(notification);
                if (published) {
                    LOG.warn("There is an index called [" + deflector.getName() + "]. Cannot fix this automatically and published a notification.");
                }
            } else {
                deflector.setUp();
            }
        } else {
            try {
                String currentTarget = deflector.getCurrentActualTargetIndex();
                String shouldBeTarget = deflector.getNewestTargetName();

                if (!shouldBeTarget.equals(currentTarget)) {
                    String msg = "Deflector is pointing to [" + currentTarget + "], not the newest one: [" + shouldBeTarget + "]. Re-pointing.";
                    LOG.warn(msg);
                    activityWriter.write(new Activity(msg, IndexRotationThread.class));

                    if (ClusterHealthStatus.RED == indices.waitForRecovery(shouldBeTarget)) {
                        LOG.error("New target index for deflector didn't get healthy within timeout. Skipping deflector update.");
                    } else {
                        deflector.pointTo(shouldBeTarget, currentTarget);
                    }
                }
            } catch (NoTargetIndexException e) {
                LOG.warn("Deflector is not up. Not trying to point to another index.");
            }
        }

    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 10;
    }

}
