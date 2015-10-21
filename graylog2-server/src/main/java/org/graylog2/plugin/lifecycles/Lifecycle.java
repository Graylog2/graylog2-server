/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
