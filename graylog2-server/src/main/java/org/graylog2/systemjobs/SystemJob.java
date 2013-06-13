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
package org.graylog2.systemjobs;

import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class SystemJob {

    public abstract void execute();
    public abstract void requestCancel();
    public abstract int getProgress();

    public abstract boolean providesProgress();
    public abstract boolean isCancelable();
    public abstract String getDescription();

    protected Core server;
    protected String id;

    public void prepare(Core server) {
        this.server = server;
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

}
