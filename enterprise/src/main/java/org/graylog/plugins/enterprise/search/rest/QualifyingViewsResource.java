package org.graylog.plugins.enterprise.search.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.enterprise.search.views.QualifyingViewsService;
import org.graylog.plugins.enterprise.search.views.ViewParameterSummaryDTO;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.stream.Collectors;

@Api(value = "Enterprise/Views/QualifyingViews", description = "List qualifying views for view interlinking")
@Path("/views/forValue")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@RequiresPermissions(EnterpriseSearchRestPermissions.VIEW_USE)
public class QualifyingViewsResource extends RestResource implements PluginRestResource {
    private final QualifyingViewsService qualifyingViewsService;

    @Inject
    public QualifyingViewsResource(QualifyingViewsService qualifyingViewsService) {
        this.qualifyingViewsService = qualifyingViewsService;
    }

    @POST
    @ApiOperation("Get all views that match given parameter value")
    @NoAuditEvent("Only returning matching views, not changing any data")
    public Collection<ViewParameterSummaryDTO> forParameter() {
        return qualifyingViewsService.forValue()
                .stream()
                .filter(view -> isPermitted(EnterpriseSearchRestPermissions.VIEW_READ, view.id()))
                .collect(Collectors.toSet());
    }
}
