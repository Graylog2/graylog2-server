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
import com.google.common.collect.Lists;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import io.krakens.grok.api.exception.GrokException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.rest.models.tools.requests.GrokTestRequest;
import org.graylog2.rest.resources.tools.responses.GrokTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Path("/tools/grok_tester")
@Produces(MediaType.APPLICATION_JSON)
public class GrokTesterResource extends RestResource {

    private final GrokPatternService grokPatternService;

    @Inject
    public GrokTesterResource(GrokPatternService grokPatternService) {
        this.grokPatternService = grokPatternService;
    }

    @GET
    @Timed
    public GrokTesterResponse grokTest(@QueryParam("pattern") @NotEmpty String pattern,
                                       @QueryParam("string") @NotNull String string,
                                       @QueryParam("named_captures_only") @NotNull boolean namedCapturesOnly) throws GrokException {

        return doTestGrok(string, pattern, namedCapturesOnly);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to test Grok patterns")
    public GrokTesterResponse testGrok(@Valid @NotNull GrokTestRequest grokTestRequest) throws GrokException {
        return doTestGrok(grokTestRequest.string(), grokTestRequest.pattern(), grokTestRequest.namedCapturesOnly());
    }

    private GrokTesterResponse doTestGrok(String string, String pattern, boolean namedCapturesOnly) throws GrokException {
        final Set<GrokPattern> grokPatterns = grokPatternService.loadAll();

        final GrokCompiler grokCompiler = GrokCompiler.newInstance();
        for (GrokPattern grokPattern : grokPatterns) {
            grokCompiler.register(grokPattern.name(), grokPattern.pattern());
        }

        final Grok grok;
        try {
            grok = grokCompiler.compile(pattern, namedCapturesOnly);
        } catch (Exception e) {
            return GrokTesterResponse.createError(pattern, string, e.getMessage());
        }

        final Match match = grok.match(string);
        final Map<String, Object> matches = match.captureFlattened();

        final GrokTesterResponse response;
        if (matches.isEmpty()) {
            response = GrokTesterResponse.createSuccess(false, Collections.<GrokTesterResponse.Match>emptyList(), pattern, string);
        } else {
            final List<GrokTesterResponse.Match> responseMatches = Lists.newArrayList();
            for (final Map.Entry<String, Object> entry : matches.entrySet()) {
                final Object value = entry.getValue();
                if (value != null) {
                    responseMatches.add(GrokTesterResponse.Match.create(entry.getKey(), value.toString()));
                }
            }

            response = GrokTesterResponse.createSuccess(true, responseMatches, pattern, string);
        }
        return response;
    }
}
