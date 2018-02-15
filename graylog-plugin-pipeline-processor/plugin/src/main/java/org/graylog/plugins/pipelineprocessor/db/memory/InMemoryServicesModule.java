package org.graylog.plugins.pipelineprocessor.db.memory;

import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.plugin.PluginModule;

public class InMemoryServicesModule extends PluginModule {
    @Override
    protected void configure() {
        bind(RuleService.class).to(InMemoryRuleService.class).asEagerSingleton();
        bind(PipelineService.class).to(InMemoryPipelineService.class).asEagerSingleton();
        bind(PipelineStreamConnectionsService.class).to(InMemoryPipelineStreamConnectionsService.class).asEagerSingleton();
    }
}
