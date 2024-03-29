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

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistCheckRequest;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistCheckResponse;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistRegexGenerationRequest;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistRegexGenerationResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.urlwhitelist.RegexHelper;
import org.graylog2.system.urlwhitelist.UrlWhitelist;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/UrlWhitelist", tags = {CLOUD_VISIBLE})
@Path("/system/urlwhitelist")
@Produces(MediaType.APPLICATION_JSON)
public class UrlWhitelistResource extends RestResource {

    private final UrlWhitelistService urlWhitelistService;
    private final RegexHelper regexHelper;

    @Inject
    public UrlWhitelistResource(final UrlWhitelistService urlWhitelistService, RegexHelper regexHelper) {
        this.urlWhitelistService = urlWhitelistService;
        this.regexHelper = regexHelper;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get url whitelist.")
    @RequiresPermissions(RestPermissions.URL_WHITELIST_READ)
    public UrlWhitelist get() {
        checkPermission(RestPermissions.URL_WHITELIST_READ);
        return urlWhitelistService.getWhitelist();
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update url whitelist.")
    @AuditEvent(type = AuditEventTypes.URL_WHITELIST_UPDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.URL_WHITELIST_WRITE)
    public Response put(@ApiParam(name = "whitelist", required = true) @NotNull final UrlWhitelist whitelist) {
        urlWhitelistService.saveWhitelist(whitelist);
        return Response.noContent().build();
    }

    @POST
    @Path("/check")
    @Timed
    @ApiOperation(value = "Check if a url is whitelisted.")
    @NoAuditEvent("Validation only")
    @Consumes(MediaType.APPLICATION_JSON)
    // Checking can be done without any special permission.
    public WhitelistCheckResponse check(@ApiParam(name = "JSON body", required = true)
                                        @Valid @NotNull final WhitelistCheckRequest checkRequest) {
        final boolean isWhitelisted = urlWhitelistService.isWhitelisted(checkRequest.url());
        return WhitelistCheckResponse.create(checkRequest.url(), isWhitelisted);
    }

    @POST
    @Path("/generate_regex")
    @Timed
    @ApiOperation(value = "Generates a regex that can be used as a value for a whitelist entry.")
    @NoAuditEvent("Utility function only.")
    @Consumes(MediaType.APPLICATION_JSON)
    public WhitelistRegexGenerationResponse generateRegex(@ApiParam(name = "JSON body", required = true)
                                                          @Valid @NotNull final WhitelistRegexGenerationRequest generationRequest) {
        final String regex;
        if (generationRequest.placeholder() == null) {
            regex = regexHelper.createRegexForUrl(generationRequest.urlTemplate());
        } else {
            regex = regexHelper.createRegexForUrlTemplate(generationRequest.urlTemplate(),
                    generationRequest.placeholder());
        }
        return WhitelistRegexGenerationResponse.create(regex);
    }
}
