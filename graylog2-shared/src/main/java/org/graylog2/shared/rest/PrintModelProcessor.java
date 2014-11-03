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
package org.graylog2.shared.rest;

import com.google.common.base.Joiner;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Configuration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PrintModelProcessor implements ModelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger("REST API");

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
        LOG.debug("Map for resource model <" + resourceModel + ">:");
        final List<Resource> resources = new ArrayList<>();

        for (Resource resource : resourceModel.getResources()) {
            resources.add(resource);
            resources.addAll(findChildResources(resource));
        }

        logResources(resources);

        return resourceModel;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        LOG.debug("Map for sub-resource model <" + subResourceModel + ">:");
        logResources(subResourceModel.getResources());

        return subResourceModel;
    }

    private void logResources(List<Resource> resources) {
        final List<ResourceDescription> resourceDescriptions = new ArrayList<>();
        for (Resource resource : resources) {
            for (ResourceMethod resourceMethod : resource.getAllMethods()) {
                String path = resource.getPath();
                Resource parent = resource.getParent();
                while (parent != null) {
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }

                    path = parent.getPath() + path;
                    parent = parent.getParent();
                }

                resourceDescriptions.add(new ResourceDescription(resourceMethod.getHttpMethod(), path, resource.getHandlerClasses()));
            }
        }

        Collections.sort(resourceDescriptions);
        for (ResourceDescription resource : resourceDescriptions) {
            LOG.debug(resource.toString());
        }
    }

    private List<Resource> findChildResources(Resource parentResource) {
        final List<Resource> childResources = new ArrayList<>();
        for (Resource resource : parentResource.getChildResources()) {
            childResources.add(resource);
            childResources.addAll(findChildResources(resource));
        }

        return childResources;
    }

    private static class ResourceDescription implements Comparable<ResourceDescription> {
        private String method;
        private String path;
        private Set<Class<?>> handlerClasses;

        private ResourceDescription(String method, String path, Set<Class<?>> handlerClasses) {
            this.method = method;
            this.path = path;
            this.handlerClasses = handlerClasses;
        }

        @Override
        public int compareTo(final ResourceDescription o) {
            if (this.path.compareTo(o.path) == 0) {
                return this.method.compareTo(o.method);
            } else {
                return this.path.compareTo(o.path);
            }
        }

        @Override
        public String toString() {
            return String.format("    %-7s %s (%s)", method, path, Joiner.on(", ").join(handlerClasses));
        }
    }
}
