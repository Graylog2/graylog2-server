/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.lifecycles;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Lifecycle {

    public static final Lifecycle UNINITIALIZED = new Lifecycle("Uninitialized", LoadBalancerStatus.DEAD);
    public static final Lifecycle STARTING = new Lifecycle("Starting", LoadBalancerStatus.DEAD);
    public static final Lifecycle RUNNING = new Lifecycle("Running", LoadBalancerStatus.ALIVE);
    public static final Lifecycle PAUSED = new Lifecycle("Paused", LoadBalancerStatus.DEAD);
    public static final Lifecycle HALTING = new Lifecycle("Halting", LoadBalancerStatus.DEAD);

    private final String name;
    private final LoadBalancerStatus loadBalancerStatus;

    protected Lifecycle(String name, LoadBalancerStatus loadBalancerStatus) {
        this.name = name;
        this.loadBalancerStatus = loadBalancerStatus;
    }

    public LoadBalancerStatus getLoadbalancerStatus() {
        return loadBalancerStatus;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " [LB:" + getLoadbalancerStatus() + "]";
    }

}
