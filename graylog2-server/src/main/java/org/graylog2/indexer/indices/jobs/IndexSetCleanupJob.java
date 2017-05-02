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
package org.graylog2.indexer.indices.jobs;

import com.google.inject.assistedinject.Assisted;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.MongoIndexRangeService;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicLong;

public class IndexSetCleanupJob extends SystemJob {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetCleanupJob.class);
    private static final int MAX_CONCURRENCY = 1_000;

    public interface Factory {
        IndexSetCleanupJob create(IndexSet indexSet);
    }

    private final Indices indices;
    private final MongoIndexRangeService indexRangeService;
    private final IndexSet indexSet;

    private volatile boolean cancel;
    private volatile long total = 0L;
    private final AtomicLong deleted = new AtomicLong(0L);

    @Inject
    public IndexSetCleanupJob(final Indices indices, final MongoIndexRangeService indexRangeService, @Assisted final IndexSet indexSet) {
        this.indices = indices;
        this.indexRangeService = indexRangeService;
        this.indexSet = indexSet;
        this.cancel = false;
    }

    @Override
    public void execute() {
        final IndexSetConfig config = indexSet.getConfig();
        final String[] managedIndices = indexSet.getManagedIndices();

        this.total = managedIndices.length;

        try {
            LOG.info("Deleting index template <{}> from Elasticsearch", config.indexTemplateName());
            indices.deleteIndexTemplate(indexSet);
        } catch (Exception e) {
            LOG.error("Unable to delete index template <{}>", config.indexTemplateName(), e);
        }

        for (String indexName : managedIndices) {
            if (cancel) {
                LOG.info("Cancel requested. Deleted <{}> of <{}> indices.", deleted, total);
                break;
            }
            try {
                LOG.info("Removing index range information for index: {}", indexName);
                indexRangeService.remove(indexName);

                LOG.info("Deleting index <{}> in index set <{}> ({})", indexName, config.id(), config.title());
                indices.delete(indexName);
                deleted.incrementAndGet();
            } catch (Exception e) {
                LOG.error("Unable to delete index <{}>", indexName, e);
            }
        }
    }

    @Override
    public void requestCancel() {
        this.cancel = true;
    }

    @Override
    public int getProgress() {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.floor((deleted.floatValue() / (float) total) * 100);
    }

    @Override
    public int maxConcurrency() {
        return MAX_CONCURRENCY;
    }

    @Override
    public boolean providesProgress() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Deletes all indices in an index set.";
    }

    @Override
    public String getClassName() {
        return getClass().getCanonicalName();
    }
}
