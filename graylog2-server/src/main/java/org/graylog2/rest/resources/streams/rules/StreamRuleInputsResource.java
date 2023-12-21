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
package org.graylog2.rest.resources.streams.rules;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.resources.streams.rules.responses.StreamRuleInputsList;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.input.StreamRuleInputsProvider;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "StreamRulesInputs", description = "Provide stream rule inputs", tags = {CLOUD_VISIBLE})
@Path("/streams/rules/inputs")
public class StreamRuleInputsResource extends RestResource {


    private final Set<StreamRuleInputsProvider> streamRuleInputsProviders;

    @Inject
    public StreamRuleInputsResource(Set<StreamRuleInputsProvider> streamRuleInputsProviders) {
        this.streamRuleInputsProviders = streamRuleInputsProviders;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all inputs for stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.INPUTS_READ)
    public StreamRuleInputsList list() throws NotFoundException {
        return StreamRuleInputsList.create(streamRuleInputsProviders.stream()
                .map(StreamRuleInputsProvider::inputs)
                .flatMap(Set::stream)
                .collect(Collectors.toSet()));
    }

}
