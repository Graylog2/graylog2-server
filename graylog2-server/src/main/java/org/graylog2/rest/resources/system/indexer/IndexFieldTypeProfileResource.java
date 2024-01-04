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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileData;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.graylog2.audit.AuditEventTypes.INDEX_FIELD_TYPE_PROFILE_CREATE;
import static org.graylog2.audit.AuditEventTypes.INDEX_FIELD_TYPE_PROFILE_DELETE;
import static org.graylog2.audit.AuditEventTypes.INDEX_FIELD_TYPE_PROFILE_UPDATE;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/IndexSets/FieldTypeProfiles", tags = {CLOUD_VISIBLE})
@Path("/system/indices/index_sets/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IndexFieldTypeProfileResource extends RestResource {

    private final IndexFieldTypeProfileService profileService;

    @Inject
    public IndexFieldTypeProfileResource(final IndexFieldTypeProfileService profileService) {
        this.profileService = profileService;
    }

    @GET
    @Path("/{profile_id}")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets profile by id")
    public IndexFieldTypeProfile retrieveById(@ApiParam(name = "profile_id") @PathParam("profile_id") String profileId) {
        return profileService.get(profileId)
                .orElseThrow(() -> new NotFoundException("No profile with id : " + profileId));
    }

    @GET
    @Path("/paginated")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets profile by id")
    public PageListResponse<IndexFieldTypeProfile> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                           @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                           @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                           @ApiParam(name = "filters") @QueryParam("filters") List<String> filters,
                                                           @ApiParam(name = "sort",
                                                                     value = "The field to sort the result on",
                                                                     required = true,
                                                                     allowableValues = "name")
                                                           @DefaultValue(IndexFieldTypeProfile.NAME_FIELD_NAME) @QueryParam("sort") String sort,
                                                           @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                           @DefaultValue("asc") @QueryParam("order") String order) {
        return profileService.getPaginated(query, filters, page, perPage, sort, order);
    }

    @POST
    @Timed
    @AuditEvent(type = INDEX_FIELD_TYPE_PROFILE_CREATE)
    @ApiOperation(value = "Creates a new profile")
    public IndexFieldTypeProfile create(@ApiParam(name = "profileData") IndexFieldTypeProfileData profileData) {
        return profileService.save(new IndexFieldTypeProfile(profileData));
    }

    @PUT
    @Timed
    @AuditEvent(type = INDEX_FIELD_TYPE_PROFILE_UPDATE)
    @ApiOperation(value = "Updates existing profile")
    public void update(@ApiParam(name = "profile") IndexFieldTypeProfile profile) {
        final boolean updated = profileService.update(profile.id(), profile);
        if (!updated) {
            throw new NotFoundException("Profile does not exist : " + profile.id());
        }
    }

    @DELETE
    @Path("/{profile_id}")
    @Timed
    @AuditEvent(type = INDEX_FIELD_TYPE_PROFILE_DELETE)
    @ApiOperation(value = "Removes a profile")
    public void delete(@ApiParam(name = "profile_id") @PathParam("profile_id") String profileId) {
        profileService.delete(profileId);
    }

}
