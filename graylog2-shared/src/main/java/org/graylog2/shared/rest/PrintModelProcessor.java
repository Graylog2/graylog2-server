/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.shared.rest;

import com.google.common.base.Joiner;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.uri.PathPattern;

import javax.ws.rs.core.Configuration;

public class PrintModelProcessor implements ModelProcessor {
    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
        System.out.println("Map for resource model <" + resourceModel + ">:");
        for (Resource resource : resourceModel.getResources()) {
            for (ResourceMethod resourceMethod : resource.getAllMethods()) {
                System.out.println(formatEndpoint(
                        resourceMethod.getHttpMethod(),
                        resource.getPathPattern(),
                        resource.getHandlerClasses()));
            }
        }

        return resourceModel;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        System.out.println("Map for sub-resource model <" + subResourceModel + ">:");
        for (Resource resource : subResourceModel.getResources()) {
            for (ResourceMethod resourceMethod : resource.getAllMethods()) {
                System.out.println(formatEndpoint(
                        resourceMethod.getHttpMethod(),
                        resource.getPathPattern(),
                        resource.getHandlerClasses()));
            }
        }

        return subResourceModel;
    }

    private String formatEndpoint(final String method, final PathPattern pathPattern, Iterable<Class<?>> handlerClasses) {
        return String.format("    %-7s %s (%s)", method, pathPattern, Joiner.on(", ").join(handlerClasses));
    }

}
