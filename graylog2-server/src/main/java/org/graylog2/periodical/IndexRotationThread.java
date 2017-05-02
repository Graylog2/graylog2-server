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

import io.searchbox.cluster.Health;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class IndexRotationThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRotationThread.class);

    private NotificationService notificationService;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;
    private final ActivityWriter activityWriter;
    private final Indices indices;
    private final NodeId nodeId;
    private final Map<String, Provider<RotationStrategy>> rotationStrategyMap;

    @Inject
    public IndexRotationThread(NotificationService notificationService,
                               Indices indices,
                               IndexSetRegistry indexSetRegistry,
                               Cluster cluster,
                               ActivityWriter activityWriter,
                               NodeId nodeId,
                               Map<String, Provider<RotationStrategy>> rotationStrategyMap) {
        this.notificationService = notificationService;
        this.indexSetRegistry = indexSetRegistry;
        this.cluster = cluster;
        this.activityWriter = activityWriter;
        this.indices = indices;
        this.nodeId = nodeId;
        this.rotationStrategyMap = rotationStrategyMap;
    }

    @Override
    public void doRun() {
        // Point deflector to a new index if required.
        if (cluster.isConnected()) {
            indexSetRegistry.forEach((indexSet) -> {
                try {
                    if (indexSet.getConfig().isWritable()) {
                        checkAndRepair(indexSet);
                        checkForRotation(indexSet);
                    } else {
                        LOG.debug("Skipping non-writable index set <{}> ({})", indexSet.getConfig().id(), indexSet.getConfig().title());
                    }
                } catch (Exception e) {
                    LOG.error("Couldn't point deflector to a new index", e);
                }
            });
        } else {
            LOG.debug("Elasticsearch cluster isn't healthy. Skipping index rotation.");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected void checkForRotation(IndexSet indexSet) {
        final IndexSetConfig config = indexSet.getConfig();
        final Provider<RotationStrategy> rotationStrategyProvider = rotationStrategyMap.get(config.rotationStrategyClass());

        if (rotationStrategyProvider == null) {
            LOG.warn("Rotation strategy \"{}\" not found, not running index rotation!", config.rotationStrategyClass());
            rotationProblemNotification("Index Rotation Problem!",
                    "Index rotation strategy " + config.rotationStrategyClass() + " not found! Please fix your index rotation configuration!");
            return;
        }

        final RotationStrategy rotationStrategy = rotationStrategyProvider.get();

        if (rotationStrategy == null) {
            LOG.warn("No rotation strategy found, not running index rotation!");
            return;
        }

        rotationStrategy.rotate(indexSet);
    }

    private void rotationProblemNotification(String title, String description) {
        final Notification notification = notificationService.buildNow()
                .addNode(nodeId.toString())
                .addType(Notification.Type.GENERIC)
                .addSeverity(Notification.Severity.URGENT)
                .addDetail("title", title)
                .addDetail("description", description);
        notificationService.publishIfFirst(notification);
    }

    protected void checkAndRepair(IndexSet indexSet) {
        if (!indexSet.isUp()) {
            if (indices.exists(indexSet.getWriteIndexAlias())) {
                // Publish a notification if there is an *index* called graylog2_deflector
                Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.DEFLECTOR_EXISTS_AS_INDEX)
                        .addSeverity(Notification.Severity.URGENT);
                final boolean published = notificationService.publishIfFirst(notification);
                if (published) {
                    LOG.warn("There is an index called [" + indexSet.getWriteIndexAlias() + "]. Cannot fix this automatically and published a notification.");
                }
            } else {
                indexSet.setUp();
            }
        } else {
            try {
                String currentTarget;
                try {
                    currentTarget = indexSet.getActiveWriteIndex();
                } catch (TooManyAliasesException e) {
                    // If we get this exception, there are multiple indices which have the deflector alias set.
                    // We try to cleanup the alias and try again. This should not happen, but might under certain
                    // circumstances.
                    indexSet.cleanupAliases(e.getIndices());
                    try {
                        currentTarget = indexSet.getActiveWriteIndex();
                    } catch (TooManyAliasesException e1) {
                        throw new IllegalStateException(e1);
                    }
                }
                String shouldBeTarget = indexSet.getNewestIndex();

                if (!shouldBeTarget.equals(currentTarget)) {
                    String msg = "Deflector is pointing to [" + currentTarget + "], not the newest one: [" + shouldBeTarget + "]. Re-pointing.";
                    LOG.warn(msg);
                    activityWriter.write(new Activity(msg, IndexRotationThread.class));

                    if (Health.Status.RED == indices.waitForRecovery(shouldBeTarget)) {
                        LOG.error("New target index for deflector didn't get healthy within timeout. Skipping deflector update.");
                    } else {
                        indexSet.pointTo(shouldBeTarget, currentTarget);
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
