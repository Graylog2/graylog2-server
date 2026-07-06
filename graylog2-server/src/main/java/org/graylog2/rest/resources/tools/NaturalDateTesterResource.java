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
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.utilities.date.NaturalDateParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiresAuthentication
@Path("/tools/natural_date_tester")
public class NaturalDateTesterResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(NaturalDateTesterResource.class);

    public record NaturalDateResponse(
            @JsonProperty("from") DateTime from,
            @JsonProperty("to") DateTime to,
            @JsonProperty("timezone") String timezone
    ) {}

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public NaturalDateResponse naturalDateTester(@QueryParam("string") @NotEmpty final String string, @QueryParam("timezone") @NotEmpty final String timezone) {
        try {
            final NaturalDateParser.Result result = new NaturalDateParser(timezone).parse(string);
            return new NaturalDateResponse(result.getFrom(), result.getTo(), result.getDateTimeZone().getID());
        } catch (NaturalDateParser.DateNotParsableException e) {
            LOG.debug("Could not parse from natural date: " + string + " and TimeZone: " + timezone, e);
            throw new WebApplicationException(e, 422);
        }
    }
}
