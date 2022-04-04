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
package org.graylog2.rest.resources.system;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.zafarkhaja.semver.expr.LexerException;
import com.google.auto.value.AutoValue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.configuration.validators.SearchVersionRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Locale;

@Api(value = "System/SearchVersion", description = "Checks system search version requirements")
@Path("/system/searchVersion")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SearchVersionResource extends RestResource implements PluginRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SearchVersionResource.class);
    final private ElasticsearchVersionProvider versionProvider;

    @Inject
    public SearchVersionResource(ElasticsearchVersionProvider versionProvider) {
        this.versionProvider = versionProvider;
    }

    @GET
    @Path("/satisfiesVersion/{distribution}")
    @ApiOperation(value = "Confirms whether the current search version satisfies a given distribution and an optional Semantic Versioning version")
    public SatisfiesVersionResponse satisfiesVersion(@ApiParam(name = "distribution", required = true) @PathParam("distribution") String distribution,
                                                     @ApiParam(name = "version") @QueryParam("version") String version) {
        // if no version provided give default to only check distribution
        if (version == null || version.isEmpty()) {
            version = ">0";
        }

        // attempt to parse a SearchVersion.Distribution from provided distribution string
        final SearchVersion.Distribution requiredDistribution;
        try {
            requiredDistribution = SearchVersion.Distribution.valueOf(distribution.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            LOG.error("Unsupported distribution {}. Valid values are [opensearch, elasticsearch].", distribution);
            throw new InternalServerErrorException(StringUtils.f(
                    "Unsupported distribution %s. Valid values are [opensearch, elasticsearch].", distribution));
        }

        final SearchVersion currentVersion = versionProvider.get();
        final SearchVersionRange requiredVersion = SearchVersionRange.of(requiredDistribution, version);
        final boolean satisfied;
        try {
            LOG.debug("Checking current version {} satisfies required version {} {}",
                    currentVersion, requiredDistribution, version);
            satisfied = currentVersion.satisfies(requiredVersion);
        } catch (LexerException e) {
            // catch invalid SemVer expression
            LOG.error("Unable to create a search version range for SemVer expression {}", version);
            throw new InternalServerErrorException(
                    StringUtils.f("Unable to create a search version range for SemVer expression %s", version));
        }

        // create and send response
        String errorMessage = "";
        if (!satisfied) {
            errorMessage = StringUtils.f("Current search version %s does not satisfy required version %s %s",
                    currentVersion, requiredDistribution, version);
        }
        return SatisfiesVersionResponse.Builder.create().satisfied(satisfied).errorMessage(errorMessage).build();
    }
}

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SatisfiesVersionResponse.Builder.class)
abstract class SatisfiesVersionResponse {

    @JsonProperty("satisfied")
    public abstract boolean satisfied();

    @JsonProperty("errorMessage")
    public abstract String errorMessage();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("satisfied")
        public abstract SatisfiesVersionResponse.Builder satisfied(boolean satisfied);

        @JsonProperty("errorMessage")
        public abstract SatisfiesVersionResponse.Builder errorMessage(String errorMessage);

        public abstract SatisfiesVersionResponse build();

        @JsonCreator
        public static SatisfiesVersionResponse.Builder create() {
            return new AutoValue_SatisfiesVersionResponse.Builder();
        }
    }
}
