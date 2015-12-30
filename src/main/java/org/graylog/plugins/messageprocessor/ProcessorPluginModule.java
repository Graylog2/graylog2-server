package org.graylog.plugins.messageprocessor;

import org.graylog.plugins.messageprocessor.processors.NaiveRuleProcessor;
import org.graylog.plugins.messageprocessor.rest.MessageProcessorRuleResource;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class ProcessorPluginModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        addMessageProcessor(NaiveRuleProcessor.class);

        addRestResource(MessageProcessorRuleResource.class);
    }
}
