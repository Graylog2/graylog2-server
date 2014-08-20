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
