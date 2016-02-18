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
package org.graylog2.rest.resources.system;

import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.gettingstarted.GettingStartedState;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.system.DisplayGettingStarted;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

@RequiresAuthentication
@Api(value = "System/GettingStartedGuides", description = "Getting Started guide")
@Path("/system/gettingstarted")
@Produces(MediaType.APPLICATION_JSON)
public class GettingStartedResource extends RestResource {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public GettingStartedResource(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    @ApiOperation("Check whether to display the Getting started guide for this version")
    public DisplayGettingStarted displayGettingStarted() {
        final Optional<GettingStartedState> gettingStartedState = clusterConfigService.get(GettingStartedState.class);
        if (!gettingStartedState.isPresent()) {
            return DisplayGettingStarted.create(true);
        }
        final boolean isDismissed = gettingStartedState.get().dismissedInVersions().contains(currentMinorVersionString());
        return DisplayGettingStarted.create(!isDismissed);
    }

    @POST
    @Path("dismiss")
    @ApiOperation("Dismiss auto-showing getting started guide for this version")
    public void dismissGettingStarted() {
        final Optional<GettingStartedState> gettingStartedState = clusterConfigService.get(GettingStartedState.class);
        final GettingStartedState state = gettingStartedState.orElse(GettingStartedState.create(Sets.<String>newHashSet()));
        state.dismissedInVersions().add(currentMinorVersionString());
        clusterConfigService.write(state);

    }

    private static String currentMinorVersionString() {
        return String.format("%d.%d",
                             Version.CURRENT_CLASSPATH.major,
                             Version.CURRENT_CLASSPATH.minor);
    }

}
