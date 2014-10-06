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
package org.graylog2.plugin.rest;

import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.google.common.base.Joiner;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Collection;

import static javax.ws.rs.core.Response.status;

@Provider
public class JacksonPropertyExceptionMapper implements ExtendedExceptionMapper<PropertyBindingException> {

    @Override
    public boolean isMappable(PropertyBindingException exception) {
        return exception != null;
    }

    @Override
    public Response toResponse(PropertyBindingException exception) {
        final Collection<Object> knownPropertyIds = exception.getKnownPropertyIds();
        final String message =
                "Unable to map property " + exception.getPropertyName() + ". \nKnown properties include: "
                        + (knownPropertyIds == null ? "<>" : Joiner.on(", ").join(knownPropertyIds));
        return status(Response.Status.BAD_REQUEST).entity(message).build();
    }
}
