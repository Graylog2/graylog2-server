/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.lifecycles;

public enum Lifecycle {

    UNINITIALIZED("Uninitialized", LoadBalancerStatus.DEAD),
    STARTING("Starting", LoadBalancerStatus.DEAD),
    RUNNING("Running", LoadBalancerStatus.ALIVE),
    PAUSED("Paused", LoadBalancerStatus.ALIVE),
    HALTING("Halting", LoadBalancerStatus.DEAD),
    FAILED("Failed", LoadBalancerStatus.DEAD),
    THROTTLED("Throttled", LoadBalancerStatus.THROTTLED),

    // Manual lifecycle override, usually set by REST calls.
    OVERRIDE_LB_DEAD("Override lb:DEAD", LoadBalancerStatus.DEAD),
    OVERRIDE_LB_ALIVE("Override lb:ALIVE", LoadBalancerStatus.ALIVE),
    OVERRIDE_LB_THROTTLED("Override lb:THROTTLED", LoadBalancerStatus.THROTTLED);

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
