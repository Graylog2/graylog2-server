/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.contentpacks.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Test;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelIdParamConverterTest extends JerseyTest {
    static {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, ModelIdParamConverter.Provider.class);
    }

    @Test
    public void testStringParam() {
        Form form = new Form();
        form.param("form", "formParam");
        final Response response = target().path("resource")
                .path("pathParam")
                .matrixParam("matrix", "matrixParam")
                .queryParam("query", "queryParam")
                .request()
                .header("header", "headerParam")
                .cookie("cookie", "cookieParam")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        final String str = response.readEntity(String.class);
        assertThat(str).isEqualTo("pathParam_matrixParam_queryParam_headerParam_cookieParam_formParam");
    }


    @Path("resource")
    public static class Resource {
        @NoAuditEvent("Test")
        @POST
        @Path("{path}")
        public String modelId(@PathParam("path") ModelId path,
                              @MatrixParam("matrix") ModelId matrix,
                              @QueryParam("query") ModelId query,
                              @HeaderParam("header") ModelId header,
                              @CookieParam("cookie") ModelId cookie,
                              @FormParam("form") ModelId form) {
            return path.id()
                    + "_" + matrix.id()
                    + "_" + query.id()
                    + "_" + header.id()
                    + "_" + cookie.id()
                    + "_" + form.id();
        }
    }
}