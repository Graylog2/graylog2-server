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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.models.system.urlallowlist.AllowlistCheckRequest;
import org.graylog2.rest.models.system.urlallowlist.AllowlistCheckResponse;
import org.graylog2.rest.models.system.urlallowlist.AllowlistRegexGenerationRequest;
import org.graylog2.rest.models.system.urlallowlist.AllowlistRegexGenerationResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.urlallowlist.RegexHelper;
import org.graylog2.system.urlallowlist.UrlAllowlist;
import org.graylog2.system.urlallowlist.UrlAllowlistService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/UrlAllowlist", tags = {CLOUD_VISIBLE})
@Path("/system/urlallowlist")
@Produces(MediaType.APPLICATION_JSON)
public class UrlAllowlistResource extends RestResource {

    private final UrlAllowlistService urlAllowlistService;
    private final RegexHelper regexHelper;

    @Inject
    public UrlAllowlistResource(final UrlAllowlistService urlAllowlistService, RegexHelper regexHelper) {
        this.urlAllowlistService = urlAllowlistService;
        this.regexHelper = regexHelper;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get url allowlist.")
    @RequiresPermissions(RestPermissions.URL_ALLOWLIST_READ)
    public UrlAllowlist get() {
        checkPermission(RestPermissions.URL_ALLOWLIST_READ);
        return urlAllowlistService.getAllowlist();
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update url allowlist.")
    @AuditEvent(type = AuditEventTypes.URL_ALLOWLIST_UPDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.URL_ALLOWLIST_WRITE)
    public Response put(@ApiParam(name = "allowlist", required = true) @NotNull final UrlAllowlist allowlist) {
        urlAllowlistService.saveAllowlist(allowlist);
        return Response.noContent().build();
    }

    @POST
    @Path("/check")
    @Timed
    @ApiOperation(value = "Check if a url is allowlisted.")
    @NoAuditEvent("Validation only")
    @Consumes(MediaType.APPLICATION_JSON)
    // Checking can be done without any special permission.
    public AllowlistCheckResponse check(@ApiParam(name = "JSON body", required = true)
                                        @Valid @NotNull final AllowlistCheckRequest checkRequest) {
        final boolean isAllowlisted = urlAllowlistService.isAllowlisted(checkRequest.url());
        return AllowlistCheckResponse.create(checkRequest.url(), isAllowlisted);
    }

    @POST
    @Path("/generate_regex")
    @Timed
    @ApiOperation(value = "Generates a regex that can be used as a value for a allowlist entry.")
    @NoAuditEvent("Utility function only.")
    @Consumes(MediaType.APPLICATION_JSON)
    public AllowlistRegexGenerationResponse generateRegex(@ApiParam(name = "JSON body", required = true)
                                                          @Valid @NotNull final AllowlistRegexGenerationRequest generationRequest) {
        final String regex;
        if (generationRequest.placeholder() == null) {
            regex = regexHelper.createRegexForUrl(generationRequest.urlTemplate());
        } else {
            regex = regexHelper.createRegexForUrlTemplate(generationRequest.urlTemplate(),
                    generationRequest.placeholder());
        }
        return AllowlistRegexGenerationResponse.create(regex);
    }
}
