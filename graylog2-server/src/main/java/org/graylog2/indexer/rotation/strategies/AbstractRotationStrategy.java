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

package org.graylog2.indexer.rotation.strategies;

import com.google.common.collect.ImmutableMap;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_ROTATION_COMPLETE;

public abstract class AbstractRotationStrategy implements RotationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRotationStrategy.class);

    public interface Result {
        String getDescription();
        boolean shouldRotate();
    }

    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;

    public AbstractRotationStrategy(AuditEventSender auditEventSender, NodeId nodeId) {
        this.auditEventSender = requireNonNull(auditEventSender);
        this.nodeId = nodeId;
    }

    @Nullable
    protected abstract Result shouldRotate(String indexName, IndexSet indexSet);

    @Override
    public void rotate(IndexSet indexSet) {
        final String strategyName = this.getClass().getCanonicalName();
        final String indexName;
        try {
            indexName = indexSet.getNewestIndex();
        } catch (NoTargetIndexException e) {
            LOG.error("Could not find current deflector target. Aborting.", e);
            return;
        }

        final Result rotate = shouldRotate(indexName, indexSet);
        if (rotate == null) {
            LOG.error("Cannot perform rotation at this moment.");
            return;
        }
        LOG.debug("Rotation strategy result: {}", rotate.getDescription());
        if (rotate.shouldRotate()) {
            LOG.info("Deflector index <{}> should be rotated, Pointing deflector to new index now!", indexName);
            indexSet.cycle();
            auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_ROTATION_COMPLETE, ImmutableMap.of(
                    "index_name", indexName,
                    "rotation_strategy", strategyName
            ));
        } else {
            LOG.debug("Deflector index <{}> should not be rotated. Not doing anything.", indexName);
        }
    }
}
