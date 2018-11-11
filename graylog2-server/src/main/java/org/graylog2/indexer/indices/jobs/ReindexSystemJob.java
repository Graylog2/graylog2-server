/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Graylog.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Graylog.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.indices.jobs;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.indices.ElasticsearchTaskView;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A system job that wraps the actual reindexing task.
 */
public class ReindexSystemJob extends SystemJob {
    private static final Logger LOG = LoggerFactory.getLogger(ReindexSystemJob.class);

    private final Indices indices;
    private final String indexName;
    private ElasticsearchTaskView taskView;

    public interface Factory {
        ReindexSystemJob create(String indexName);
    }

    @AssistedInject
    ReindexSystemJob(Indices indices, @Assisted String indexName) {
        this.indices = indices;
        this.indexName = indexName;
    }

    @Override
    public void execute() {
        taskView = indices.reindex(indexName, indexName+"_reindex_migrated");
        try {
            taskView.waitForCompletion();
        } catch (ElasticsearchException ee) {
            LOG.error("Unable to reindex {}: {}", indexName, ExceptionUtils.getRootCauseMessage(ee));
        }
    }

    @Override
    public void requestCancel() {
        if (taskView != null) {
            taskView.cancel();
        }
    }

    @Override
    public int getProgress() {
        if (taskView == null) {
            return 0;
        }
        if (taskView.isCompleted()) {
            return 100;
        }
        return 0;
    }

    @Override
    public int maxConcurrency() {
        // we only allow a single reindex job in the system at any given time (TODO really?)
        return 1;
    }

    @Override
    public boolean providesProgress() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        if (taskView == null) {
            return true;
        }
        return taskView.cancellable();
    }

    @Override
    public String getDescription() {
        return "Reindexing something";
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
