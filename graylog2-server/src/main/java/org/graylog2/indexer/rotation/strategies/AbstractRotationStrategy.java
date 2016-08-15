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
import org.graylog2.audit.AuditActions;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class AbstractRotationStrategy implements RotationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRotationStrategy.class);

    public interface Result {
        String getDescription();
        boolean shouldRotate();
    }

    private final Deflector deflector;
    private final AuditEventSender auditEventSender;

    public AbstractRotationStrategy(Deflector deflector, AuditEventSender auditEventSender) {
        this.deflector = requireNonNull(deflector);
        this.auditEventSender = requireNonNull(auditEventSender);
    }

    @Nullable
    protected abstract Result shouldRotate(String indexName);

    @Override
    public void rotate() {
        final String strategyName = this.getClass().getCanonicalName();
        final String indexName;
        try {
            indexName = deflector.getNewestTargetName();
        } catch (NoTargetIndexException e) {
            final ImmutableMap<String, Object> auditEventContext = ImmutableMap.of("rotation_strategy", strategyName);
            auditEventSender.failure("<system>", AuditActions.ES_INDEX_ROTATION_INITIATE, auditEventContext);

            LOG.error("Could not find current deflector target. Aborting.", e);
            return;
        }

        final Map<String, Object> auditEventContext = ImmutableMap.of(
            "index_name", indexName,
            "rotation_strategy", strategyName);
        final Result rotate = shouldRotate(indexName);
        if (rotate == null) {
            LOG.error("Cannot perform rotation at this moment.");

            auditEventSender.failure("<system>", AuditActions.ES_INDEX_ROTATION_INITIATE, auditEventContext);
            return;
        }
        LOG.debug("Rotation strategy result: {}", rotate.getDescription());
        if (rotate.shouldRotate()) {
            LOG.info("Deflector index <{}> should be rotated, Pointing deflector to new index now!", indexName);
            deflector.cycle();
            auditEventSender.success("<system>", AuditActions.ES_INDEX_ROTATION_COMPLETE, auditEventContext);
        } else {
            LOG.debug("Deflector index <{}> should not be rotated. Not doing anything.", indexName);
        }
    }
}
