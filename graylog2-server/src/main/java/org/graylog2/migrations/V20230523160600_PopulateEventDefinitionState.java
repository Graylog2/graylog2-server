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
package org.graylog2.migrations;

import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.systemnotification.SystemNotificationEventEntityScope;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class V20230523160600_PopulateEventDefinitionState extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230523160600_PopulateEventDefinitionState.class);

    private final ClusterConfigService clusterConfigService;
    private final DBEventDefinitionService dbEventDefinitionService;
    private final DBJobDefinitionService dbJobDefinitionService;

    @Inject
    public V20230523160600_PopulateEventDefinitionState(ClusterConfigService clusterConfigService,
                                                        DBEventDefinitionService dbEventDefinitionService,
                                                        DBJobDefinitionService dbJobDefinitionService) {
        this.clusterConfigService = clusterConfigService;
        this.dbEventDefinitionService = dbEventDefinitionService;
        this.dbJobDefinitionService = dbJobDefinitionService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-26T16:06:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20230523160600_PopulateEventDefinitionState.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }
        // Collect a list of all event definitions with a defined job (ie, enabled event definitions) as well as all
        // system event definitions to be marked as enabled
        List<String> enabledEventDefinitionIds = new ArrayList<>();
        dbEventDefinitionService.streamAll().forEach(dto -> {
            Optional<JobDefinitionDto> jobDefinition = dbJobDefinitionService.getByConfigField(EventDto.FIELD_EVENT_DEFINITION_ID, dto.id());
            if (dto.scope().equals(SystemNotificationEventEntityScope.NAME) || jobDefinition.isPresent()) {
                enabledEventDefinitionIds.add(dto.id());
            }
        });

        // Mark enabled event definitions as such
        enabledEventDefinitionIds.forEach(id -> dbEventDefinitionService.updateState(id, EventDefinition.State.ENABLED));
        clusterConfigService.write(new V20230523160600_PopulateEventDefinitionState.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
