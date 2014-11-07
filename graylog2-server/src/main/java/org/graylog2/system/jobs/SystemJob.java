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
package org.graylog2.system.jobs;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.Map;

public abstract class SystemJob {

    private final ServerStatus serverStatus;

    // Known types that can be resolved in the SystemJobFactory.
    public enum Type {
        FIX_DEFLECTOR_DELETE_INDEX,
        FIX_DEFLECTOR_MOVE_INDEX
    }

    public abstract void execute();

    public abstract void requestCancel();

    public abstract int getProgress();

    public abstract int maxConcurrency();

    public abstract boolean providesProgress();

    public abstract boolean isCancelable();

    public abstract String getDescription();

    public abstract String getClassName();

    //protected Core core;
    protected String id;
    protected DateTime startedAt;

    protected SystemJob(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    protected SystemJob() {
        this.serverStatus = null;
    }

    public String getId() {
        if (id == null) {
            throw new IllegalStateException("Cannot return ID if the job has not been started yet.");
        }

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void markStarted() {
        startedAt = Tools.iso8601();
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public Map<String, Object> toMap() {
        return ImmutableMap.<String, Object>builder()
                .put("id", id)
                .put("name", getClassName()) // getting the concrete class, not this abstract one
                .put("description", getDescription())
                .put("started_at", Tools.getISO8601String(getStartedAt()))
                .put("percent_complete", getProgress())
                .put("provides_progress", providesProgress())
                .put("is_cancelable", isCancelable())
                .put("node_id", serverStatus.getNodeId().toString())
                .build();
    }

}
