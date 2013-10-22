/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.indexer.indices.jobs;

import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import org.graylog2.Core;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class OptimizeIndexJob extends SystemJob {

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeIndexJob.class);

    public static final int MAX_CONCURRENCY = 1000;

    private final String index;

    public OptimizeIndexJob(Core core, String index) {
        this.core = core;
        this.index = index;
    }

    @Override
    public void execute() {
        String msg = "Optimizing index <" + index + ">.";
        core.getActivityWriter().write(new Activity(msg, OptimizeIndexJob.class));
        LOG.info(msg);

        // http://www.elasticsearch.org/guide/reference/api/admin-indices-optimize/
        OptimizeRequest or = new OptimizeRequest(index);

        or.maxNumSegments(1);
        or.onlyExpungeDeletes(false);
        or.flush(true);
        or.waitForMerge(true); // This makes us block until the operation finished.

        core.getIndexer().getClient().admin().indices().optimize(or).actionGet();
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
        return "Optimises and index for read performance.";
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }

}
