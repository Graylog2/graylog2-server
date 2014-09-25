/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.rest.resources;

import org.glassfish.jersey.server.model.Resource;
import org.graylog2.plugin.rest.PluginRestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Path("/plugin")
public class PluginResource {

    private final Map<String, Set<PluginRestResource>> pluginRestResources;

    @Inject
    public PluginResource(Map<String, Set<PluginRestResource>> pluginRestResources) {
        this.pluginRestResources = pluginRestResources;
    }

    @Path("{plugin}/{resource}")
    public PluginRestResource getResource(@PathParam("plugin") final String pluginId, @PathParam("resource") String resource) {
        final Set<PluginRestResource> pluginResources = pluginRestResources.get(pluginId);
        if (pluginResources == null || pluginResources.size() == 0)
            throw new NotFoundException();

        System.out.println("pluginId = " + pluginId + ", resource = " + resource);
        if (!resource.startsWith("/"))
            resource = "/" + resource;

        for (PluginRestResource pluginRestResource : pluginResources) {
            System.out.println("Checking " + pluginRestResource);
            Path pathAnnotation = Resource.getPath(pluginRestResource.getClass());
            System.out.println("PathAnnotation: " + pathAnnotation);
            if (pathAnnotation != null && pathAnnotation.value() != null) {
                String pathAnnotationString = pathAnnotation.value();
                if (!pathAnnotationString.startsWith("/"))
                    pathAnnotationString = "/" + pathAnnotationString;
                if (pathAnnotationString.equals(resource)) {
                    System.out.println("Returning " + pluginRestResource);
                    return pluginRestResource;
                }
            }
        }

        throw new NotFoundException();
    }
}
