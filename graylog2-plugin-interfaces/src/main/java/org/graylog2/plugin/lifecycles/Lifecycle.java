/*
 * Copyright 2012-2014 TORCH GmbH
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
package org.graylog2.plugin.lifecycles;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public enum Lifecycle {

    UNINITIALIZED("Uninitialized", LoadBalancerStatus.DEAD),
    STARTING("Starting", LoadBalancerStatus.DEAD),
    RUNNING("Running", LoadBalancerStatus.ALIVE),
    PAUSED("Paused", LoadBalancerStatus.ALIVE),
    HALTING("Halting", LoadBalancerStatus.DEAD),
    FAILED("Failed", LoadBalancerStatus.DEAD),

    // Manual lifecycle override, usually set by REST calls.
    OVERRIDE_LB_DEAD("Override lb:DEAD", LoadBalancerStatus.DEAD),
    OVERRIDE_LB_ALIVE("Override lb:ALIVE", LoadBalancerStatus.ALIVE);

    private final String description;
    private final LoadBalancerStatus loadBalancerStatus;

    Lifecycle(String description, LoadBalancerStatus loadBalancerStatus) {
        this.description = description;
        this.loadBalancerStatus = loadBalancerStatus;
    }

    public LoadBalancerStatus getLoadbalancerStatus() {
        return loadBalancerStatus;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description + " [LB:" + getLoadbalancerStatus() + "]";
    }

}
