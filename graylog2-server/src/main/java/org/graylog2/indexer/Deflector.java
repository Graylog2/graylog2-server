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
package org.graylog2.indexer;

import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.indices.InvalidAliasNameException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

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
    // TODO: Hardcoded archive suffix. See: https://github.com/Graylog2/graylog2-server/issues/2058
    public static final String RESTORED_ARCHIVE_SUFFIX = "_restored_archive";

    private final SystemJobManager systemJobManager;
    private final ActivityWriter activityWriter;
    private final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory;
    private final String indexPrefix;
    private final String deflectorName;
    private final Indices indices;
    private final SetIndexReadOnlyJob.Factory indexReadOnlyJobFactory;
    private final Pattern deflectorIndexPattern;
    private final Pattern indexPattern;

    @Inject
    public Deflector(final SystemJobManager systemJobManager,
                     @Named("elasticsearch_index_prefix") final String indexPrefix,
                     final ActivityWriter activityWriter,
                     final SetIndexReadOnlyJob.Factory indexReadOnlyJobFactory,
                     final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory,
                     final Indices indices) {
        this.indexPrefix = indexPrefix;

        this.systemJobManager = systemJobManager;
        this.activityWriter = activityWriter;
        this.indexReadOnlyJobFactory = indexReadOnlyJobFactory;
        this.createNewSingleIndexRangeJobFactory = createNewSingleIndexRangeJobFactory;

        this.deflectorName = buildName(indexPrefix);
        this.indices = indices;
        this.deflectorIndexPattern = Pattern.compile("^" + indexPrefix + SEPARATOR + "\\d+");
        this.indexPattern = Pattern.compile("^" + indexPrefix + SEPARATOR + "\\d+(?:"+ RESTORED_ARCHIVE_SUFFIX +")?");
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

            // Do we have a target index to point to?
            try {
                final String currentTarget = getNewestTargetName();
                LOG.info("Pointing to already existing index target <{}>", currentTarget);

                pointTo(currentTarget);
            } catch (NoTargetIndexException ex) {
                final String msg = "There is no index target to point to. Creating one now.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, Deflector.class));

                cycle(); // No index, so automatically cycling to a new one.
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

        LOG.info("Waiting for index allocation of <{}>", newTarget);
        ClusterHealthStatus healthStatus = indices.waitForRecovery(newTarget);
        LOG.debug("Health status of index <{}>: {}", newTarget, healthStatus);

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
            addSingleIndexRanges(oldTarget);

            // perform these steps after a delay, so we don't race with indexing into the alias
            // it can happen that an index request still writes to the old deflector target, while we cycled it above.
            // setting the index to readOnly would result in ClusterBlockExceptions in the indexing request.
            // waiting 30 seconds to perform the background task should completely get rid of these errors.
            final SystemJob makeReadOnlyJob = indexReadOnlyJobFactory.create(oldTarget);
            try {
                systemJobManager.submitWithDelay(makeReadOnlyJob, 30, TimeUnit.SECONDS);
            } catch (SystemJobConcurrencyException e) {
                LOG.error("Cannot set index <" + oldTarget + "> to read only. It won't be optimized.", e);
            }
            activity.setMessage("Cycled deflector from <" + oldTarget + "> to <" + newTarget + ">");
        }

        addSingleIndexRanges(newTarget);

        LOG.info("Done!");

        activityWriter.write(activity);
    }

    public int getNewestTargetNumber() throws NoTargetIndexException {
        final Set<String> indexNames = indices.getIndexNamesAndAliases(getDeflectorWildcard()).keySet();

        if (indexNames.isEmpty()) {
            throw new NoTargetIndexException();
        }

        int highestIndexNumber = -1;
        for (String indexName : indexNames) {
            if (!isGraylogDeflectorIndex(indexName)) {
                continue;
            }

            try {
                final int indexNumber = extractIndexNumber(indexName);
                highestIndexNumber = Math.max(indexNumber, highestIndexNumber);
            } catch (NumberFormatException ex) {
                LOG.warn("Couldn't extract index number from index name " + indexName, ex);
            }
        }

        if (highestIndexNumber == -1) {
            throw new NoTargetIndexException();
        }

        return highestIndexNumber;
    }

    /**
     * Returns a list of all Graylog managed indices.
     *
     * @return list of managed indices
     */
    public String[] getAllGraylogIndexNames() {
        final Set<String> indexNames = indices.getIndexNamesAndAliases(getDeflectorWildcard()).keySet();
        // also allow restore archives to be returned
        final List<String> result = indexNames.stream()
                .filter(this::isGraylogIndex)
                .collect(Collectors.toList());

        return result.toArray(new String[result.size()]);
    }

    /**
     * Returns all Graylog deflector indices.
     *
     * @return index name and aliases of that index
     */
    public Map<String, Set<String>> getAllGraylogDeflectorIndices() {
        final Map<String, Set<String>> indexNamesAndAliases = indices.getIndexNamesAndAliases(getDeflectorWildcard());

        // filter out the restored archives from the result set
        return indexNamesAndAliases.entrySet().stream()
                .filter(e -> isGraylogDeflectorIndex(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    private void addSingleIndexRanges(String indexName) {
        try {
            systemJobManager.submit(createNewSingleIndexRangeJobFactory.create(this, indexName));
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Could not calculate index ranges for index " + indexName + " after cycling deflector: Maximum concurrency of job is reached.";
            activityWriter.write(new Activity(msg, Deflector.class));
            LOG.error(msg, e);
        }
    }

    @Nullable
    public String getCurrentActualTargetIndex() {
        return indices.aliasTarget(getName());
    }

    public String getName() {
        return deflectorName;
    }

    public String getDeflectorWildcard() {
        return indexPrefix + SEPARATOR + "*";
    }

    public boolean isDeflectorAlias(final String indexName) {
        return getName().equals(indexName);
    }

    public boolean isGraylogDeflectorIndex(final String indexName) {
        return !isNullOrEmpty(indexName) && !isDeflectorAlias(indexName) && deflectorIndexPattern.matcher(indexName).matches();
    }

    public boolean isGraylogIndex(final String indexName) {
        return !isNullOrEmpty(indexName) && !isDeflectorAlias(indexName) && indexPattern.matcher(indexName).matches();
    }
}
