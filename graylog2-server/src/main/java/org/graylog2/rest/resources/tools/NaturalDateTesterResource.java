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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.utilities.date.NaturalDateParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotEmpty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@RequiresAuthentication
@Path("/tools/natural_date_tester")
public class NaturalDateTesterResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RegexTesterResource.class);

    @AutoValue
    public abstract static class NaturalDateResponse {
        @JsonProperty
        public abstract DateTime from();

        @JsonProperty
        public abstract DateTime to();

        @JsonProperty
        public abstract String timezone();

        static NaturalDateResponse create(NaturalDateParser.Result result) {
            return new AutoValue_NaturalDateTesterResource_NaturalDateResponse(result.getFrom(), result.getTo(), result.getDateTimeZone().getID());
        }
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public NaturalDateResponse naturalDateTester(@QueryParam("string") @NotEmpty final String string, @QueryParam("timezone") @NotEmpty final String timezone) {
        try {
            final NaturalDateParser.Result result = new NaturalDateParser(timezone).parse(string);
            return NaturalDateResponse.create(result);
        } catch (NaturalDateParser.DateNotParsableException e) {
            LOG.debug("Could not parse from natural date: " + string + " and TimeZone: " + timezone, e);
            throw new WebApplicationException(e, 422);
        }
    }
}
