/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.rotation;

import com.google.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class MessageCountRotationStrategy implements RotationStrategy {
    private static final Logger log = LoggerFactory.getLogger(MessageCountRotationStrategy.class);

    private final Indices indices;
    private int maxDocsPerIndex;

    @Inject
    public MessageCountRotationStrategy(Configuration configuration, Indices indices) {
        this.indices = indices;
        maxDocsPerIndex = configuration.getElasticSearchMaxDocsPerIndex();
    }

    @Override
    public RotationStrategy.Result shouldRotate(String index) {
        try {
            final long numberOfMessages = indices.numberOfMessages(index);
            return new Result(index,
                              numberOfMessages,
                              maxDocsPerIndex,
                              numberOfMessages > maxDocsPerIndex);
        } catch (IndexNotFoundException e) {
            log.error("Unknown index, cannot perform rotation", e);
            return null;
        }
    }

    private static class Result implements RotationStrategy.Result {

        public static final MessageFormat ROTATE_FORMAT = new MessageFormat(
                "Number of messages in <{0}> ({1}) is higher than the limit ({2}). Pointing deflector to new index now!");
        public static final MessageFormat NOT_ROTATE_FORMAT = new MessageFormat(
                "Number of messages in <{0}> ({1}) is lower than the limit ({2}). Not doing anything.");
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
