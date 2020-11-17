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
import org.graylog2.rest.models.tools.requests.ContainsStringTestRequest;
import org.graylog2.rest.models.tools.responses.ContainsStringTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RequiresAuthentication
@Path("/tools/contains_string_tester")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ContainsStringTesterResource extends RestResource {

    @GET
    @Timed
    public ContainsStringTesterResponse containsStringTest(@QueryParam("string") @NotEmpty String string,
                                      @QueryParam("search_string") @NotEmpty String searchString) {
        return doTestContainsString(string, searchString);
    }


    @POST
    @Timed
    @NoAuditEvent("only used to test if field contains a string")
    public ContainsStringTesterResponse testContainsString(@Valid @NotNull ContainsStringTestRequest request) {
        return doTestContainsString(request.string(), request.searchString());
    }

    private ContainsStringTesterResponse doTestContainsString(String string, String searchString) {
        final int index = string.indexOf(searchString);
        final boolean contains = index != -1;
        final ContainsStringTesterResponse.Match match;
        if (contains) {
            match = ContainsStringTesterResponse.Match.create(index, index + searchString.length());
        } else {
            match = null;
        }

        return ContainsStringTesterResponse.create(contains, match, searchString, string);
    }
}
