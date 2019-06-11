package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.search.views.QualifyingViewsService;
import org.graylog.plugins.views.search.views.ViewParameterSummaryDTO;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharing;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
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
@RequiresPermissions(EnterpriseSearchRestPermissions.VIEW_USE)
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

                    return isPermitted(EnterpriseSearchRestPermissions.VIEW_READ, view.id())
                            || viewSharing.map(sharing -> isViewSharedForUser.isAllowedToSee(getCurrentUser(), sharing)).orElse(false);
                })
                .collect(Collectors.toSet());
    }
}
