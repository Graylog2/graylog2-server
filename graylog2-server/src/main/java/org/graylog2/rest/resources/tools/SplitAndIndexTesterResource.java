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
import org.graylog2.inputs.extractors.SplitAndIndexExtractor;
import org.graylog2.rest.models.tools.requests.SplitAndIndexTestRequest;
import org.graylog2.rest.models.tools.responses.SplitAndIndexTesterResponse;
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
@Path("/tools/split_and_index_tester")
public class SplitAndIndexTesterResource extends RestResource {
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public SplitAndIndexTesterResponse splitAndIndexTester(@QueryParam("split_by") @NotNull String splitBy,
                                                           @QueryParam("index") @Min(0) int index,
                                                           @QueryParam("string") @NotNull String string) {
        return doSplitAndIndexTest(string, splitBy, index);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to test split and index extractor")
    public SplitAndIndexTesterResponse splitAndIndexTest(@Valid @NotNull SplitAndIndexTestRequest splitAndIndexTestRequest) {
        return doSplitAndIndexTest(splitAndIndexTestRequest.string(),
                splitAndIndexTestRequest.splitBy(), splitAndIndexTestRequest.index());
    }

    private SplitAndIndexTesterResponse doSplitAndIndexTest(String string, String splitBy, int index) {
        final String cut = SplitAndIndexExtractor.cut(string, splitBy, index - 1);
        int[] positions = SplitAndIndexExtractor.getCutIndices(string, splitBy, index - 1);

        return SplitAndIndexTesterResponse.create(cut != null, cut, positions[0], positions[1]);
    }
}
