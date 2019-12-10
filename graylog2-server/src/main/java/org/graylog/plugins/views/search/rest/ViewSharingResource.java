/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.UserShortSummary;
import org.graylog.plugins.views.search.views.sharing.ViewSharing;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "Enterprise/Views", description = "View Sharing management")
@Path("/views/{id}/share")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class ViewSharingResource extends RestResource implements PluginRestResource {
    private final ViewSharingService viewSharingService;
    private final ViewService viewService;
    private final UserService userService;

    @Inject
    public ViewSharingResource(ViewSharingService viewSharingService, ViewService viewService, UserService userService) {
        this.viewSharingService = viewSharingService;
        this.viewService = viewService;
        this.userService = userService;
    }

    @GET
    @ApiOperation("Get the sharing configuration for this view")
    public ViewSharing get(@ApiParam @PathParam("id") @NotEmpty String id) {
        ensureUserIsPermittedForView(id);
        return viewSharingService.forView(id).orElseThrow(NotFoundException::new);
    }

    @POST
    @ApiOperation("Configure sharing for a view")
    @AuditEvent(type = ViewsAuditEventTypes.VIEW_SHARING_CREATE)
    public ViewSharing create(@ApiParam @PathParam("id") @NotEmpty String id, ViewSharing viewSharing) {
        ensureUserIsPermittedForView(id);
        checkPermission(ViewsRestPermissions.VIEW_EDIT, id);
        return viewSharingService.create(viewSharing);
    }

    @DELETE
    @ApiOperation("Delete sharing of a view")
    @AuditEvent(type = ViewsAuditEventTypes.VIEW_SHARING_DELETE)
    public ViewSharing delete(@ApiParam @PathParam("id") @NotEmpty String id) {
        ensureUserIsPermittedForView(id);
        checkPermission(ViewsRestPermissions.VIEW_EDIT, id);
        return viewSharingService.remove(id).orElse(null);
    }

    @GET
    @Path("/users")
    @ApiOperation("Get a list of summaries of available users for sharing")
    public Set<UserShortSummary> summarizeUsers(@ApiParam @PathParam("id") @NotEmpty String id) {
        final List<User> users = userService.loadAll();
        final String currentUser = getCurrentUser() != null ? getCurrentUser().getName() : null;
        return users.stream()
                .filter(user -> !user.getName().equals(currentUser))
                .map(user -> UserShortSummary.create(user.getName(), user.getFullName()))
                .collect(Collectors.toSet());
    }

    private void ensureUserIsPermittedForView(String viewId) {
        viewService.get(viewId).orElseThrow(NotFoundException::new);
        checkPermission(ViewsRestPermissions.VIEW_READ, viewId);
    }
}
