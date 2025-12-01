package org.graylog2.indexer.indices.jobs;

import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class IndexJobsModule extends PluginModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        addSchedulerJob(SetIndexReadOnlyAndCalculateRangeJob.TYPE_NAME,
                SetIndexReadOnlyAndCalculateRangeJob.class,
                SetIndexReadOnlyAndCalculateRangeJob.Factory.class,
                SetIndexReadOnlyAndCalculateRangeJob.Config.class,
                SetIndexReadOnlyAndCalculateRangeJob.Data.class
        );
    }
}
