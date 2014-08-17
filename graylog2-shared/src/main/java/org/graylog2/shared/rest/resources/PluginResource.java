/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
