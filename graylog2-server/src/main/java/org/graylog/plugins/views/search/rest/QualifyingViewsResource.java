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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.views.QualifyingViewsService;
import org.graylog.plugins.views.search.views.ViewParameterSummaryDTO;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharing;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Api(value = "Enterprise/Views/QualifyingViews", description = "List qualifying views for view interlinking")
@Path("/views/forValue")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class QualifyingViewsResource extends RestResource implements PluginRestResource {
    private final QualifyingViewsService qualifyingViewsService;
    private final ViewSharingService viewSharingService;
    private final IsViewSharedForUser isViewSharedForUser;

    @Inject
    public QualifyingViewsResource(QualifyingViewsService qualifyingViewsService,
                                   ViewSharingService viewSharingService,
                                   IsViewSharedForUser isViewSharedForUser) {
        this.qualifyingViewsService = qualifyingViewsService;
        this.viewSharingService = viewSharingService;
        this.isViewSharedForUser = isViewSharedForUser;
    }

    @POST
    @ApiOperation("Get all views that match given parameter value")
    @NoAuditEvent("Only returning matching views, not changing any data")
    public Collection<ViewParameterSummaryDTO> forParameter() {
        return qualifyingViewsService.forValue()
                .stream()
                .filter(view -> {
                    final Optional<ViewSharing> viewSharing = viewSharingService.forView(view.id());

                    return isPermitted(ViewsRestPermissions.VIEW_READ, view.id())
                            || viewSharing.map(sharing -> isViewSharedForUser.isAllowedToSee(getCurrentUser(), sharing)).orElse(false);
                })
                .collect(Collectors.toSet());
    }
}
