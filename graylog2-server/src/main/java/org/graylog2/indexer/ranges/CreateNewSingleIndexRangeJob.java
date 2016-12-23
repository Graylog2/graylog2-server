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
package org.graylog2.indexer.ranges;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class CreateNewSingleIndexRangeJob extends RebuildIndexRangesJob {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNewSingleIndexRangeJob.class);
    private final String indexName;

    public interface Factory {
        CreateNewSingleIndexRangeJob create(Set<IndexSet> indexSets, String indexName);
    }

    @AssistedInject
    public CreateNewSingleIndexRangeJob(@Assisted Set<IndexSet> indexSets,
                                        @Assisted String indexName,
                                        ActivityWriter activityWriter,
                                        IndexRangeService indexRangeService) {
        super(indexSets, activityWriter, indexRangeService);
        this.indexName = checkNotNull(indexName);
    }

    @Override
    public String getDescription() {
        return "Creates new single index range information.";
    }

    @Override
    public String getInfo() {
        return "Calculating ranges for index " + indexName + ".";
    }

    @Override
    public void execute() {
        LOG.info("Calculating ranges for index {}.", indexName);
        try {
            final IndexRange indexRange = indexRangeService.calculateRange(indexName);
            indexRangeService.save(indexRange);
            LOG.info("Created ranges for index {}.", indexName);
        } catch (Exception e) {
            LOG.error("Exception during index range calculation for index " + indexName, e);
        }
    }

    @Override
    public boolean providesProgress() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public int maxConcurrency() {
        // Actually we need some sort of queuing for SystemJobs.
        return Integer.MAX_VALUE;
    }
}
