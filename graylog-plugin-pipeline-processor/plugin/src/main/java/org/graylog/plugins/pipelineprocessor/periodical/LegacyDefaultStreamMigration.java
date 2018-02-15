package org.graylog.plugins.pipelineprocessor.periodical;

import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.events.LegacyDefaultStreamMigrated;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class LegacyDefaultStreamMigration extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyDefaultStreamMigration.class);
    private final ClusterConfigService clusterConfigService;
    private final PipelineStreamConnectionsService connectionsService;

    private static final String LEGACY_STREAM_ID = "default";

    @Inject
    public LegacyDefaultStreamMigration(ClusterConfigService clusterConfigService, PipelineStreamConnectionsService connectionsService) {
        this.clusterConfigService = clusterConfigService;
        this.connectionsService = connectionsService;
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        final LegacyDefaultStreamMigrated migrationState =
                clusterConfigService.getOrDefault(LegacyDefaultStreamMigrated.class,
                        LegacyDefaultStreamMigrated.create(false));
        return !migrationState.migrationDone();
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        try {
            final PipelineConnections defaultConnections = connectionsService.load(LEGACY_STREAM_ID);
            connectionsService.save(defaultConnections.toBuilder().streamId(Stream.DEFAULT_STREAM_ID).build());
            connectionsService.delete(LEGACY_STREAM_ID);
            clusterConfigService.write(LegacyDefaultStreamMigrated.create(true));
            LOG.info("Pipeline connections to legacy default streams migrated successfully.");
        } catch (NotFoundException e) {
            LOG.info("Legacy default stream has no connections, no migration needed.");
        }
    }
}
