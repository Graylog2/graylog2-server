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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.indices.InvalidAliasNameException;
import org.graylog2.Configuration;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Format of actual indexes behind the Deflector:
 * [configured_prefix]_1
 * [configured_prefix]_2
 * [configured_prefix]_3
 * ...
 */
public class Deflector { // extends Ablenkblech
    private static final Logger LOG = LoggerFactory.getLogger(Deflector.class);

    public static final String DEFLECTOR_SUFFIX = "deflector";
    public static final String SEPARATOR = "_";

    private final SystemJobManager systemJobManager;
    private final ActivityWriter activityWriter;
    private final RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory;
    private final OptimizeIndexJob.Factory optimizeIndexJobFactory;
    private final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory;
    private final String indexPrefix;
    private final String deflectorName;
    private final Indices indices;
    private final Configuration configuration;

    @Inject
    public Deflector(final SystemJobManager systemJobManager,
                     final Configuration configuration,
                     final ActivityWriter activityWriter,
                     final RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory,
                     final OptimizeIndexJob.Factory optimizeIndexJobFactory,
                     final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory,
                     final Indices indices) {
        this.configuration = configuration;
        indexPrefix = configuration.getElasticSearchIndexPrefix();

        this.systemJobManager = systemJobManager;
        this.activityWriter = activityWriter;
        this.rebuildIndexRangesJobFactory = rebuildIndexRangesJobFactory;
        this.optimizeIndexJobFactory = optimizeIndexJobFactory;
        this.createNewSingleIndexRangeJobFactory = createNewSingleIndexRangeJobFactory;

        this.deflectorName = buildName(configuration.getElasticSearchIndexPrefix());
        this.indices = indices;
    }

    public boolean isUp() {
        return indices.aliasExists(getName());
    }

    public void setUp() {
        // Check if there already is an deflector index pointing somewhere.
        if (isUp()) {
            LOG.info("Found deflector alias <{}>. Using it.", getName());
        } else {
            LOG.info("Did not find an deflector alias. Setting one up now.");

            try {
                // Do we have a target index to point to?
                try {
                    String currentTarget = getNewestTargetName();
                    LOG.info("Pointing to already existing index target <{}>", currentTarget);

                    pointTo(currentTarget);
                } catch(NoTargetIndexException ex) {
                    final String msg = "There is no index target to point to. Creating one now.";
                    LOG.info(msg);
                    activityWriter.write(new Activity(msg, Deflector.class));

                    cycle(); // No index, so automatically cycling to a new one.
            }
            } catch (InvalidAliasNameException e) {
                LOG.error("Seems like there already is an index called [{}]", getName());
            }
        }
    }

    public void cycle() {
        LOG.info("Cycling deflector to next index now.");
        int oldTargetNumber;

        try {
            oldTargetNumber = getNewestTargetNumber();
        } catch (NoTargetIndexException ex) {
            oldTargetNumber = -1;
        }
        final int newTargetNumber = oldTargetNumber + 1;

        final String newTarget = buildIndexName(indexPrefix, newTargetNumber);
        final String oldTarget = buildIndexName(indexPrefix, oldTargetNumber);

        if (oldTargetNumber == -1) {
            LOG.info("Cycling from <none> to <{}>", newTarget);
        } else {
            LOG.info("Cycling from <{}> to <{}>", oldTarget, newTarget);
        }

        // Create new index.
        LOG.info("Creating index target <{}>...", newTarget);
        if (!indices.create(newTarget)) {
            LOG.error("Could not properly create new target <{}>", newTarget);
        }

        LOG.info("Done!");

        // Point deflector to new index.
        LOG.info("Pointing deflector to new target index....");

        final Activity activity = new Activity(Deflector.class);
        if (oldTargetNumber == -1) {
            // Only pointing, not cycling.
            pointTo(newTarget);
            activity.setMessage("Cycled deflector from <none> to <" + newTarget + ">");
        } else {
            // Re-pointing from existing old index to the new one.
            pointTo(newTarget, oldTarget);
            LOG.info("Flushing old index <{}>.", oldTarget);
            indices.flush(oldTarget);

            LOG.info("Setting old index <{}> to read-only.", oldTarget);
            indices.setReadOnly(oldTarget);
            activity.setMessage("Cycled deflector from <" + oldTarget + "> to <" + newTarget + ">");

            if (!configuration.isDisableIndexOptimization()) {
                try {
                    systemJobManager.submit(optimizeIndexJobFactory.create(oldTarget));
                } catch (SystemJobConcurrencyException e) {
                    // The concurrency limit is very high. This should never happen.
                    LOG.error("Cannot optimize index <" + oldTarget + ">.", e);
                }
            }
        }

        if (configuration.isDisableIndexRangeCalculation() && oldTargetNumber != -1) {
            addSingleIndexRanges(oldTarget);
            addSingleIndexRanges(newTarget);
        } else {
            updateIndexRanges();
        }

        LOG.info("Done!");

        activityWriter.write(activity);
    }

    public int getNewestTargetNumber() throws NoTargetIndexException {
        final Map<String, IndexStats> indices = this.indices.getAll();
        if (indices.isEmpty()) {
            throw new NoTargetIndexException();
        }

        final List<Integer> indexNumbers = Lists.newArrayListWithExpectedSize(indices.size());
        for (String indexName : indices.keySet()) {
            if (!isGraylog2Index(indexName)) {
                continue;
            }

            try {
                indexNumbers.add(extractIndexNumber(indexName));
            } catch (NumberFormatException ex) {
                continue;
            }
        }

        if (indexNumbers.isEmpty()) {
            throw new NoTargetIndexException();
        }

        return Collections.max(indexNumbers);
    }

    public String[] getAllDeflectorIndexNames() {
        final List<String> result = Lists.newArrayListWithExpectedSize(indices.getAll().size());
        for (String indexName : indices.getAll().keySet()) {
            if (isGraylog2Index(indexName)) {
                result.add(indexName);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public Map<String, IndexStats> getAllDeflectorIndices() {
        final ImmutableMap.Builder<String, IndexStats> result = ImmutableMap.builder();

        if (indices != null) {
            for (Map.Entry<String, IndexStats> e : indices.getAll().entrySet()) {
                final String name = e.getKey();

                if (isGraylog2Index(name)) {
                    result.put(name, e.getValue());
                }
            }
        }
        return result.build();
    }

    public String getNewestTargetName() throws NoTargetIndexException {
        return buildIndexName(indexPrefix, getNewestTargetNumber());
    }

    public static String buildIndexName(final String prefix, final int number) {
        return prefix + SEPARATOR + number;
    }

    public static String buildName(final String prefix) {
        return prefix + SEPARATOR + DEFLECTOR_SUFFIX;
    }

    public static int extractIndexNumber(final String indexName) throws NumberFormatException {
        final String[] parts = indexName.split(SEPARATOR);

        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            LOG.debug("Could not extract index number from index <" + indexName + ">.", e);
            throw new NumberFormatException();
        }
    }

    public void pointTo(final String newIndex, final String oldIndex) {
        indices.cycleAlias(getName(), newIndex, oldIndex);
    }

    public void pointTo(final String newIndex) {
        indices.cycleAlias(getName(), newIndex);
    }

    private void updateIndexRanges() {
        // Re-calculate index ranges.
        try {
            systemJobManager.submit(rebuildIndexRangesJobFactory.create(this));
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Could not re-calculate index ranges after cycling deflector: Maximum concurrency of job is reached.";
            activityWriter.write(new Activity(msg, Deflector.class));
            LOG.error(msg, e);
        }
    }

    private void addSingleIndexRanges(String indexName) {
        try {
            systemJobManager.submit(createNewSingleIndexRangeJobFactory.create(this, indexName));
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Could not calculate index ranges for index " + indexName + " after cycling deflector: Maximum concurrency of job is reached.";
            activityWriter.write(new Activity(msg, Deflector.class));
            LOG.error(msg, e);
        }
    }

    public String getCurrentActualTargetIndex() {
        return indices.aliasTarget(getName());
    }

    public String getName() {
        return deflectorName;
    }

    public boolean isDeflectorAlias(final String indexName) {
        return getName().equals(indexName);
    }

    public boolean isGraylog2Index(final String indexName) {
        return !isDeflectorAlias(indexName) && indexName.startsWith(indexPrefix + SEPARATOR);
    }
}
