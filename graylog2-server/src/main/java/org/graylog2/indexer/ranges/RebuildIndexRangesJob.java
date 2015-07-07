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

import com.google.common.base.Stopwatch;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.Deflector;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RebuildIndexRangesJob extends SystemJob {
    public interface Factory {
        RebuildIndexRangesJob create(Deflector deflector);
    }

    private static final Logger LOG = LoggerFactory.getLogger(RebuildIndexRangesJob.class);
    private static final int MAX_CONCURRENCY = 1;

    private volatile boolean cancelRequested = false;
    private volatile int indicesToCalculate = 0;
    private volatile int indicesCalculated = 0;

    protected final Deflector deflector;
    private final ActivityWriter activityWriter;
    protected final IndexRangeService indexRangeService;

    @AssistedInject
    public RebuildIndexRangesJob(@Assisted Deflector deflector,
                                 ActivityWriter activityWriter,
                                 IndexRangeService indexRangeService) {
        this.deflector = deflector;
        this.activityWriter = activityWriter;
        this.indexRangeService = indexRangeService;
    }

    @Override
    public void requestCancel() {
        this.cancelRequested = true;
    }

    @Override
    public int getProgress() {
        if (indicesToCalculate <= 0) {
            return 0;
        }

        // lolwtfbbqcasting
        return (int) Math.floor(((float) indicesCalculated / (float) indicesToCalculate) * 100);
    }

    @Override
    public String getDescription() {
        return "Rebuilds index range information.";
    }

    @Override
    public void execute() {
        info("Re-calculating index ranges.");

        String[] indices = deflector.getAllDeflectorIndexNames();
        if (indices == null || indices.length == 0) {
            info("No indices, nothing to calculate.");
            return;
        }
        indicesToCalculate = indices.length;

        Stopwatch sw = Stopwatch.createStarted();
        for (String index : indices) {
            if (cancelRequested) {
                info("Stop requested. Not calculating next index range, not updating ranges.");
                sw.stop();
                return;
            }

            try {
                final IndexRange indexRange = indexRangeService.calculateRange(index);
                indexRangeService.save(indexRange);
                LOG.debug("Created ranges for index {}: {}", index, indexRange);
            } catch (Exception e) {
                LOG.info("Could not calculate range of index [" + index + "]. Skipping.", e);
            } finally {
                indicesCalculated++;
            }
        }

        info("Done calculating index ranges for " + indices.length + " indices. Took " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms.");
    }

    protected void info(String what) {
        LOG.info(what);
        activityWriter.write(new Activity(what, RebuildIndexRangesJob.class));
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
    public int maxConcurrency() {
        return MAX_CONCURRENCY;
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
