/*
 * Copyright 2013-2014 TORCH GmbH
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
 */

package org.graylog2.shared;

import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerStatus {
    private final NodeId nodeId;
    private Lifecycle lifecycle;
    private final DateTime startedAt;

    public ServerStatus(BaseConfiguration configuration) {
        this.nodeId = new NodeId(configuration.getNodeIdFile());
        this.lifecycle = Lifecycle.UNINITIALIZED;
        this.startedAt = new DateTime(DateTimeZone.UTC);
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public DateTime getStartedAt() {
        return startedAt;
    }
}
