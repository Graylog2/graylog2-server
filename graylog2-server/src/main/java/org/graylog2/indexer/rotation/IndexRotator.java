package org.graylog2.indexer.rotation;

import com.google.common.collect.ImmutableMap;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static java.util.Objects.requireNonNull;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_ROTATION_COMPLETE;

public class IndexRotator {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRotator.class);

    private final Indices indices;
    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;

    @Inject
    public IndexRotator(Indices indices, AuditEventSender auditEventSender, NodeId nodeId) {
        this.indices = indices;
        this.auditEventSender = auditEventSender;
        this.nodeId = nodeId;
    }

    public static Result createResult(boolean shouldRotate, String message) {
        return new Result(shouldRotate, message);
    }

    public void rotate(IndexSet indexSet, RotationChecker rotationChecker) {
        requireNonNull(indexSet, "indexSet must not be null");
        final String indexSetTitle = requireNonNull(indexSet.getConfig(), "Index set configuration must not be null").title();
        final String strategyName = this.getClass().getCanonicalName();
        final String indexName;
        try {
            indexName = indexSet.getNewestIndex();
        } catch (NoTargetIndexException e) {
            LOG.error("Could not find current deflector target of index set <{}>. Aborting.", indexSetTitle, e);
            return;
        }

        // Refresh so we have current stats on idle indices
        indices.refresh(indexName);

        final Result rotate = rotationChecker.shouldRotate(indexName, indexSet);
        if (rotate == null) {
            LOG.error("Cannot perform rotation of index <{}> in index set <{}> with strategy <{}> at this moment", indexName, indexSetTitle, strategyName);
            return;
        }
        LOG.debug("Rotation strategy result: {}", rotate.getDescription());
        if (rotate.shouldRotate()) {
            LOG.info("Deflector index <{}> (index set <{}>) should be rotated ({}), Pointing deflector to new index now!", indexSetTitle, indexName, rotate.getDescription());
            indexSet.cycle();
            auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_ROTATION_COMPLETE, ImmutableMap.of(
                    "index_name", indexName,
                    "rotation_strategy", strategyName
            ));
        } else {
            LOG.debug("Deflector index <{}> should not be rotated. Not doing anything.", indexName);
        }
    }

    public interface RotationChecker{
        Result shouldRotate(String indexName, IndexSet indexSet);
    }

    public static class Result {
        private final boolean shouldRotate;
        private final String message;

        public Result(boolean shouldRotate, String message) {
            this.shouldRotate = shouldRotate;
            this.message = message;
            LOG.debug("{} because of: {}", shouldRotate ? "Rotating" : "Not rotating", message);
        }

        public String getDescription() {
            return message;
        }

        public boolean shouldRotate() {
            return shouldRotate;
        }
    }

}
