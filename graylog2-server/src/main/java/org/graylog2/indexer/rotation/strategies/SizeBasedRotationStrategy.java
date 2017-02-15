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

import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;

public class SizeBasedRotationStrategy extends AbstractRotationStrategy {
    private final Indices indices;

    @Inject
    public SizeBasedRotationStrategy(Indices indices,
                                     NodeId nodeId,
                                     AuditEventSender auditEventSender) {
        super(auditEventSender, nodeId);
        this.indices = indices;
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return SizeBasedRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return SizeBasedRotationStrategyConfig.createDefault();
    }

    @Nullable
    @Override
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        if (!(indexSet.getConfig().rotationStrategy() instanceof SizeBasedRotationStrategyConfig)) {
            throw new IllegalStateException("Invalid rotation strategy config <" + indexSet.getConfig().rotationStrategy().getClass().getCanonicalName() + "> for index set <" + indexSet.getConfig().id() + ">");
        }

        final SizeBasedRotationStrategyConfig config = (SizeBasedRotationStrategyConfig) indexSet.getConfig().rotationStrategy();

        final IndexStatistics indexStats = indices.getIndexStats(index);
        if (indexStats == null) {
            return null;
        }

        final long sizeInBytes = indexStats.primaries().getStore().getSizeInBytes();

        final boolean shouldRotate = sizeInBytes > config.maxSize();

        return new Result() {
            public final MessageFormat ROTATE = new MessageFormat("Storage size for index <{0}> is {1} bytes, exceeding the maximum of {2} bytes. Rotating index.", Locale.ENGLISH);
            public final MessageFormat NOT_ROTATE = new MessageFormat("Storage size for index <{0}> is {1} bytes, below the maximum of {2} bytes. Not doing anything.", Locale.ENGLISH);

            @Override
            public String getDescription() {
                MessageFormat format = shouldRotate() ? ROTATE : NOT_ROTATE;
                return format.format(new Object[] {
                        index,
                        sizeInBytes,
                        config.maxSize()
                });
            }

            @Override
            public boolean shouldRotate() {
                return shouldRotate;
            }
        };
    }
}
