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
package org.graylog2.indexer.retention;

import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.RetentionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ClosingRetentionStrategy extends RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(ClosingRetentionStrategy.class);

    public ClosingRetentionStrategy(Indices indices) {
        super(indices);
    }

    @Override
    protected Type getType() {
        return Type.CLOSE;
    }

    @Override
    protected void doRun(String indexName) {
        LOG.debug("Closing index <{}>", indexName);
        indices.close(indexName);
    }
}
