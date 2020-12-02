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
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alerts.AlertService;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.ConfigurableTypeInfo;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.streams.alerts.AlertConditionListSummary;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;

@RequiresAuthentication
@Api(value = "AlertConditions", description = "Manage stream legacy alert conditions")
@Path("/alerts/conditions")
@Produces(MediaType.APPLICATION_JSON)
public class AlertConditionsResource extends RestResource {
    private final Map<String, AlertCondition.Factory> alertConditionTypesMap;
    private final StreamService streamService;
    private final AlertService alertService;

    @Inject
    public AlertConditionsResource(Map<String, AlertCondition.Factory> alertConditionTypesMap,
                                   StreamService streamService,
                                   AlertService alertService) {
        this.alertConditionTypesMap = alertConditionTypesMap;
        this.streamService = streamService;
        this.alertService = alertService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alert conditions")
    public AlertConditionListSummary all() {
        final List<Stream> streams = streamService.loadAll();

        final List<AlertConditionSummary> conditionSummaries = streams.stream()
                .filter(stream -> isPermitted(STREAMS_READ, stream.getId()))
                .flatMap(stream -> streamService.getAlertConditions(stream).stream()
                        .map(condition -> AlertConditionSummary.create(condition.getId(),
                                condition.getType(),
                                condition.getCreatorUserId(),
                                condition.getCreatedAt().toDate(),
                                condition.getParameters(),
                                alertService.inGracePeriod(condition),
                                condition.getTitle()))
                ).collect(Collectors.toList());

        return AlertConditionListSummary.create(conditionSummaries);
    }

    @GET
    @Path("/types")
    @Timed
    @ApiOperation(value = "Get a list of all alert condition types")
    public Map<String, ConfigurableTypeInfo> available() {
        return this.alertConditionTypesMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> ConfigurableTypeInfo.create(entry.getKey(), entry.getValue().descriptor(), entry.getValue().config().getRequestedConfiguration())
                ));
    }
}
