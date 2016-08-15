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
import org.graylog2.auditlog.AuditActions;
import org.graylog2.auditlog.AuditLogger;
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
    private final AuditLogger auditLogger;

    public AbstractRotationStrategy(Deflector deflector, AuditLogger auditLogger) {
        this.deflector = requireNonNull(deflector);
        this.auditLogger = requireNonNull(auditLogger);
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
            final ImmutableMap<String, Object> auditLogContext = ImmutableMap.of("rotation_strategy", strategyName);
            auditLogger.failure("<system>", AuditActions.ES_INDEX_ROTATION_INITIATE, auditLogContext);

            LOG.error("Could not find current deflector target. Aborting.", e);
            return;
        }

        final Map<String, Object> auditLogContext = ImmutableMap.of(
            "index_name", indexName,
            "rotation_strategy", strategyName);
        final Result rotate = shouldRotate(indexName);
        if (rotate == null) {
            LOG.error("Cannot perform rotation at this moment.");

            auditLogger.failure("<system>", AuditActions.ES_INDEX_ROTATION_INITIATE, auditLogContext);
            return;
        }
        LOG.debug("Rotation strategy result: {}", rotate.getDescription());
        if (rotate.shouldRotate()) {
            LOG.info("Deflector index <{}> should be rotated, Pointing deflector to new index now!", indexName);
            deflector.cycle();
            auditLogger.success("<system>", AuditActions.ES_INDEX_ROTATION_COMPLETE, auditLogContext);
        } else {
            LOG.debug("Deflector index <{}> should not be rotated. Not doing anything.", indexName);
        }
    }
}
