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
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileData;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileIdAndName;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileWithUsages;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

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
    public IndexFieldTypeProfileWithUsages retrieveById(@ApiParam(name = "profile_id") @PathParam("profile_id") String profileId) {
        checkPermission(RestPermissions.MAPPING_PROFILES_READ, profileId);
        return profileService.getWithUsages(profileId)
                .orElseThrow(() -> new NotFoundException("No profile with id : " + profileId));
    }

    @GET
    @Path("/paginated")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets profile by id")
    public PageListResponse<IndexFieldTypeProfileWithUsages> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
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
        checkPermission(RestPermissions.MAPPING_PROFILES_READ);
        return profileService.getPaginated(query, filters, page, perPage, sort, order);
    }

    @GET
    @Path("/all")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets list of all profiles (their ids and names only)")
    public List<IndexFieldTypeProfileIdAndName> getAll() {
        checkPermission(RestPermissions.MAPPING_PROFILES_READ);
        return profileService.getAll();
    }

    @POST
    @Timed
    @AuditEvent(type = INDEX_FIELD_TYPE_PROFILE_CREATE)
    @ApiOperation(value = "Creates a new profile")
    public IndexFieldTypeProfile create(@ApiParam(name = "profileData") IndexFieldTypeProfileData profileData) {
        checkPermission(RestPermissions.MAPPING_PROFILES_CREATE);
        return profileService.save(new IndexFieldTypeProfile(profileData));
    }

    @PUT
    @Timed
    @AuditEvent(type = INDEX_FIELD_TYPE_PROFILE_UPDATE)
    @ApiOperation(value = "Updates existing profile")
    public void update(@ApiParam(name = "profile") IndexFieldTypeProfile profile) {
        checkPermission(RestPermissions.MAPPING_PROFILES_EDIT, profile.id());
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
        checkPermission(RestPermissions.MAPPING_PROFILES_DELETE, profileId);
        profileService.delete(profileId);
    }

}
