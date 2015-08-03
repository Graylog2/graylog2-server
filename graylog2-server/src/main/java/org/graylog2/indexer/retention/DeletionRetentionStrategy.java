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
import org.graylog2.indexer.ranges.IndexRangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeletionRetentionStrategy extends RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DeletionRetentionStrategy.class);

    private final IndexRangeService indexRangeService;

    public DeletionRetentionStrategy(Indices indices, IndexRangeService indexRangeService) {
        super(indices);
        this.indexRangeService = indexRangeService;
    }

    @Override
    protected Type getType() {
        return Type.DELETE;
    }

    @Override
    protected void doRun(String indexName) {
        LOG.info("Deleting index <{}>", indexName);
        indices.delete(indexName);

        if (!indexRangeService.delete(indexName)) {
            LOG.warn("Couldn't delete index range for index <{}>", indexName);
        }
    }
}
