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
package org.graylog2.system.jobs;

import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkState;

public abstract class SystemJob {

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

    public String getInfo() {
        return "No further information available.";
    }

    protected String id;
    protected DateTime startedAt;

    public String getId() {
        checkState(id != null, "Cannot return ID if the job has not been started yet.");

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void markStarted() {
        startedAt = Tools.nowUTC();
    }

    public DateTime getStartedAt() {
        return startedAt;
    }
}
