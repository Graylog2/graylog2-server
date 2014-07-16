package org.graylog2.shared;

import org.cliffc.high_scale_lib.Counter;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface MetricsHost {
    public long getCurrentThroughput();
    public Counter getThroughputCounter();
    public void setCurrentThroughput(long x);
}
