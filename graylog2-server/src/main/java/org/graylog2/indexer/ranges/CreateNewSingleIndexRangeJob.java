package org.graylog2.indexer.ranges;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.database.ValidationException;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.EmptyIndexException;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateNewSingleIndexRangeJob extends RebuildIndexRangesJob {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNewSingleIndexRangeJob.class);
    private final String indexName;

    public interface Factory {
        public CreateNewSingleIndexRangeJob create(Deflector deflector, String indexName);
    }

    @AssistedInject
    public CreateNewSingleIndexRangeJob(@Assisted Deflector deflector,
                                        @Assisted String indexName,
                                        ServerStatus serverStatus,
                                        Indexer indexer,
                                        ActivityWriter activityWriter,
                                        IndexRangeService indexRangeService) {
        super(deflector, serverStatus, indexer, activityWriter, indexRangeService);
        this.indexName = indexName;
    }

    @Override
    public String getDescription() {
        return "Creates new single index range information.";
    }

    @Override
    public void execute() {
        LOG.info("Calculating ranges for index {}.", indexName);
        try {
            final Map<String, Object> range;
            if (deflector.getCurrentActualTargetIndex(indexer).equals(indexName))
                range = calculateRange(indexName);
            else
                range = getDeflectorIndexRange(indexName);

            final IndexRange indexRange = indexRangeService.create(range);
            indexRangeService.destroy(indexName);
            indexRangeService.save(indexRange);
            LOG.info("Created ranges for index {}.", indexName);
        } catch (EmptyIndexException e) {
            LOG.error("Unable to calculate ranges for index {}: {}", indexName, e);
        } catch (ValidationException e) {
            LOG.error("Unable to save index range for index {}: {}", indexName, e);
        } catch (Exception e) {
            LOG.error("Exception during index range calculation for index {}: ", indexName, e);
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
