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
package org.graylog2.shared.rest.resources.system.codecs;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.inputs.codecs.CodecFactory;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.rest.models.system.codecs.responses.CodecTypeInfo;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/Codecs/Types", description = "Message codec types of this node")
@Path("/system/codecs/types")
@Produces(MediaType.APPLICATION_JSON)
public class CodecTypesResource extends RestResource {
    private CodecFactory codecFactory;

    @Inject
    public CodecTypesResource(CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    @GET
    @Timed
    @Path("/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all codec types")
    public Map<String, CodecTypeInfo> getAll() {
        final Map<String, Codec.Factory<? extends Codec>> factories = codecFactory.getFactory();

        return factories
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            final Codec.Factory<? extends Codec> factory = entry.getValue();
                            return CodecTypeInfo.fromConfigurationRequest(entry.getKey(), factory.getDescriptor().getName(), factory.getConfig().getRequestedConfiguration());
                        }
                ));
    }
}
