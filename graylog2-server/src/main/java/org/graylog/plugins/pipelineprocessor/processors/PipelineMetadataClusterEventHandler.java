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
package org.graylog.plugins.pipelineprocessor.processors;

import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;

/**
 * Listens on {@link ClusterEventBus} for pipeline-related events and submits
 * {@link PipelineMetadataUpdateJob} system scheduler jobs for metadata updates.
 * Since ClusterEventBus only delivers events on the originating node, this replaces
 * the previous leader-only guard in PipelineInterpreterStateUpdater.
 */
@Singleton
public class PipelineMetadataClusterEventHandler {

    private final SystemJobManager systemJobManager;

    @Inject
    public PipelineMetadataClusterEventHandler(ClusterEventBus clusterEventBus,
                                               SystemJobManager systemJobManager) {
        this.systemJobManager = systemJobManager;
        clusterEventBus.registerClusterEventSubscriber(this);
    }

    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        systemJobManager.submit(PipelineMetadataUpdateJob.forRulesChanged(event));
    }

    @Subscribe
    public void handlePipelineChanges(PipelinesChangedEvent event) {
        systemJobManager.submit(PipelineMetadataUpdateJob.forPipelinesChanged(event));
    }

    @Subscribe
    public void handlePipelineConnectionChanges(PipelineConnectionsChangedEvent event) {
        systemJobManager.submit(PipelineMetadataUpdateJob.forConnectionsChanged(event));
    }

    @Subscribe
    public void handleInputDeleted(InputDeletedEvent event) {
        systemJobManager.submit(PipelineMetadataUpdateJob.forInputDeleted(event));
    }

    /**
     * When an input is renamed, rules that previously referenced the input by name are no longer applicable; and
     * rules that reference the new name now apply.
     * Unfortunately, we don't have an exact mapping of rules by referenced input name. So we find all the rules
     * that reference any inputs by name and fire an update event for those rules.
     * Note: we also don't have an exact mapping of functions to rules, so we just trigger for all the rules included
     * in pipelines that reference inputs in any way.
     */
    @Subscribe
    public void handleInputRenamed(InputRenamedEvent event) {
        systemJobManager.submit(PipelineMetadataUpdateJob.forInputRenamed(event));
    }
}
