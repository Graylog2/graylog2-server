/**
 * Copyright (c) 2014 Lennart Koopmann <lennart@torch.sh>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graylog2.plugin.lifecycles;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Lifecycle {

    public static final Lifecycle UNINITIALIZED = new Lifecycle("Uninitialized", LoadBalancerStatus.DEAD);
    public static final Lifecycle STARTING = new Lifecycle("Starting", LoadBalancerStatus.DEAD);
    public static final Lifecycle RUNNING = new Lifecycle("Running", LoadBalancerStatus.ALIVE);
    public static final Lifecycle PAUSED = new Lifecycle("Paused", LoadBalancerStatus.ALIVE);
    public static final Lifecycle HALTING = new Lifecycle("Halting", LoadBalancerStatus.DEAD);

    // Manual lifecycle override, usually set by REST calls.
    public static final Lifecycle OVERRIDE_LB_DEAD = new Lifecycle("Override lb:DEAD", LoadBalancerStatus.DEAD);
    public static final Lifecycle OVERRIDE_LB_ALIVE = new Lifecycle("Override lb:ALIVE", LoadBalancerStatus.ALIVE);

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
