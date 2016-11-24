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
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeIndexJob extends SystemJob {

    public interface Factory {
        OptimizeIndexJob create(String index, int maxNumSegments);
    }

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeIndexJob.class);

    public static final int MAX_CONCURRENCY = 1000;

    private final ActivityWriter activityWriter;
    private final String index;
    private final int maxNumSegments;
    private final Indices indices;

    @AssistedInject
    public OptimizeIndexJob(Indices indices,
                            ActivityWriter activityWriter,
                            @Assisted String index,
                            @Assisted int maxNumSegments) {
        this.indices = indices;
        this.activityWriter = activityWriter;
        this.index = index;
        this.maxNumSegments = maxNumSegments;
    }

    @Override
    public void execute() {
        String msg = "Optimizing index <" + index + ">.";
        activityWriter.write(new Activity(msg, OptimizeIndexJob.class));
        LOG.info(msg);

        indices.optimizeIndex(index, maxNumSegments);
    }

    @Override
    public void requestCancel() {
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int maxConcurrency() {
        return MAX_CONCURRENCY;
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
        return "Optimizes an index for read performance.";
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public String getInfo() {
        return "Optimizing index " + index + ".";
    }
}
