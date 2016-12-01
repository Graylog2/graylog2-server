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
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;

public class MessageCountRotationStrategy extends AbstractRotationStrategy {
    private static final Logger log = LoggerFactory.getLogger(MessageCountRotationStrategy.class);

    private final Indices indices;

    @Inject
    public MessageCountRotationStrategy(Indices indices, NodeId nodeId,
                                        AuditEventSender auditEventSender) {
        super(auditEventSender, nodeId);
        this.indices = indices;
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return MessageCountRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return MessageCountRotationStrategyConfig.createDefault();
    }

    @Nullable
    @Override
    protected Result shouldRotate(String index, IndexSet indexSet) {
        if (!(indexSet.getConfig().rotationStrategy() instanceof MessageCountRotationStrategyConfig)) {
            throw new IllegalStateException("Invalid rotation strategy config <" + indexSet.getConfig().rotationStrategy().getClass().getCanonicalName() + "> for index set <" + indexSet.getConfig().id() + ">");
        }

        final MessageCountRotationStrategyConfig config = (MessageCountRotationStrategyConfig) indexSet.getConfig().rotationStrategy();

        try {
            final long numberOfMessages = indices.numberOfMessages(index);
            return new Result(index,
                              numberOfMessages,
                              config.maxDocsPerIndex(),
                              numberOfMessages > config.maxDocsPerIndex());
        } catch (IndexNotFoundException e) {
            log.error("Unknown index, cannot perform rotation", e);
            return null;
        }
    }

    private static class Result implements AbstractRotationStrategy.Result {

        public static final MessageFormat ROTATE_FORMAT = new MessageFormat(
                "Number of messages in <{0}> ({1}) is higher than the limit ({2}). Pointing deflector to new index now!",
                Locale.ENGLISH);
        public static final MessageFormat NOT_ROTATE_FORMAT = new MessageFormat(
                "Number of messages in <{0}> ({1}) is lower than the limit ({2}). Not doing anything.",
                Locale.ENGLISH);
        private final String index;
        private final long actualCount;
        private final long maxDocs;
        private final boolean shouldRotate;

        public Result(String index, long actualCount, long maxDocs, boolean shouldRotate) {
            this.index = index;
            this.actualCount = actualCount;
            this.maxDocs = maxDocs;
            this.shouldRotate = shouldRotate;
        }

        @Override
        public String getDescription() {
            final MessageFormat format = (shouldRotate ? ROTATE_FORMAT : NOT_ROTATE_FORMAT);
            return format.format(new Object[]{index, actualCount, maxDocs});
        }

        @Override
        public boolean shouldRotate() {
            return shouldRotate;
        }
    }
}
