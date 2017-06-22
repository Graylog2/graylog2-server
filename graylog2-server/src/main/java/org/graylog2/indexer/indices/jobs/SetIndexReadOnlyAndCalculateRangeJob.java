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
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.SetIndexReadOnlyJob;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class SetIndexReadOnlyAndCalculateRangeJob extends SystemJob {
    private static final Logger LOG = LoggerFactory.getLogger(SetIndexReadOnlyAndCalculateRangeJob.class);

    public interface Factory {
        SetIndexReadOnlyAndCalculateRangeJob create(String indexName);
    }

    private final SetIndexReadOnlyJob.Factory setIndexReadOnlyJobFactory;
    private final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final String indexName;

    @Inject
    public SetIndexReadOnlyAndCalculateRangeJob(SetIndexReadOnlyJob.Factory setIndexReadOnlyJobFactory,
                                                CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory,
                                                IndexSetRegistry indexSetRegistry,
                                                Indices indices,
                                                @Assisted String indexName) {
        this.setIndexReadOnlyJobFactory = setIndexReadOnlyJobFactory;
        this.createNewSingleIndexRangeJobFactory = createNewSingleIndexRangeJobFactory;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.indexName = indexName;
    }

    @Override
    public void execute() {
        if (indices.isClosed(indexName)) {
            LOG.debug("Not running job for closed index <{}>", indexName);
            return;
        }
        final SystemJob setIndexReadOnlyJob = setIndexReadOnlyJobFactory.create(indexName);
        setIndexReadOnlyJob.execute();
        final SystemJob createNewSingleIndexRangeJob = createNewSingleIndexRangeJobFactory.create(indexSetRegistry.getAll(), indexName);
        createNewSingleIndexRangeJob.execute();

    }

    @Override
    public void requestCancel() {}

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int maxConcurrency() {
        return 1000;
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
    public String getDescription() {
        return "Makes index " + indexName + " read only and calculates and adds its index range afterwards.";
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
