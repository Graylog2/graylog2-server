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
package org.graylog2.rest.resources.tools;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.tools.requests.SubstringTestRequest;
import org.graylog2.rest.models.tools.responses.SubstringTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Path("/tools/substring_tester")
public class SubstringTesterResource extends RestResource {
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public SubstringTesterResponse substringTester(@QueryParam("begin_index") @Min(0) int beginIndex,
                                                   @QueryParam("end_index") @Min(1) int endIndex,
                                                   @QueryParam("string") @NotNull String string) {
        return doSubstringTest(string, beginIndex, endIndex);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used for testing substring extractor")
    public SubstringTesterResponse testSubstring(@Valid @NotNull SubstringTestRequest substringTestRequest) {
        return doSubstringTest(substringTestRequest.string(), substringTestRequest.start(), substringTestRequest.end());
    }

    private SubstringTesterResponse doSubstringTest(String string, int beginIndex, int endIndex) {
        final String cut = Tools.safeSubstring(string, beginIndex, endIndex);

        return SubstringTesterResponse.create(cut != null, cut, beginIndex, endIndex);
    }
}
