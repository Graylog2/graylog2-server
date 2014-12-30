package org.graylog2.shared.system.stats.jvm;

import javax.inject.Singleton;

@Singleton
public class JvmProbe {
    public JvmStats jvmStats() {
        return JvmStats.INSTANCE;
    }
}
