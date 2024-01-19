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
import org.graylog2.rest.models.tools.requests.RegexTestRequest;
import org.graylog2.rest.models.tools.responses.RegexTesterResponse;
import org.graylog2.rest.models.tools.responses.RegexValidationResponse;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@RequiresAuthentication
@Path("/tools/regex_tester")
public class RegexTesterResource extends RestResource {
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public RegexTesterResponse regexTester(@QueryParam("regex") @NotEmpty String regex,
                                           @QueryParam("string") @NotNull String string) {
        return doTestRegex(string, regex);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to test regex values")
    public RegexTesterResponse testRegex(@Valid @NotNull RegexTestRequest regexTestRequest) {
        return doTestRegex(regexTestRequest.string(), regexTestRequest.regex());
    }

    @GET
    @Path("/validate")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public RegexValidationResponse validateRegex(@QueryParam("regex") @NotEmpty String regex) {
        final RegexValidationResponse.Builder response = RegexValidationResponse.builder()
                .regex(regex);

        try {
            Pattern.compile(regex, Pattern.DOTALL);
            response.isValid(true);
        } catch (PatternSyntaxException e) {
            response.isValid(false).validationMessage(e.getMessage());
        }

        return response.build();
    }

    private RegexTesterResponse doTestRegex(String example, String regex) {
        final Pattern pattern;
        try {
            pattern = Pattern.compile(regex, Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            throw new BadRequestException("Invalid regular expression: " + e.getMessage(), e);
        }

        final Matcher matcher = pattern.matcher(example);
        boolean matched = matcher.find();

        // Get the first matched group.
        final RegexTesterResponse.Match match;
        if (matched && matcher.groupCount() > 0) {
            match = RegexTesterResponse.Match.create(matcher.group(1), matcher.start(1), matcher.end(1));
        } else {
            match = null;
        }

        return RegexTesterResponse.create(matched, match, regex, example);
    }
}
