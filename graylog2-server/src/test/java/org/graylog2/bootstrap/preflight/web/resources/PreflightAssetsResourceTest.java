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
package org.graylog2.bootstrap.preflight.web.resources;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.graylog2.web.resources.ResourceFileReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.activation.MimetypesFileTypeMap;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreflightAssetsResourceTest {
    private final PreflightAssetsResource resource =
            new PreflightAssetsResource(new MimetypesFileTypeMap(), new ResourceFileReader());

    /**
     * The JAX-RS path template does not match a literal slash, but percent-encoded traversal sequences pass
     * the template and are URL-decoded before they reach the resource method, which is what we get here.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "../PreflightConstants.class",
            "../../log4j2.xml",
            "../..",
            "..",
            "/etc/passwd",
            "",
            "sub/dir/app.js",
    })
    void refusesToServeAnythingOutsideOfTheAssetsDirectory(String filename) {
        assertThatThrownBy(() -> resource.get(mock(Request.class), filename))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void doesNotReflectTheRequestedFileNameBackToTheClient() {
        assertThatThrownBy(() -> resource.get(mock(Request.class), "../log4j2.xml"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageNotContaining("log4j2.xml");
    }

    @Test
    void servesAnAssetFromTheAssetsDirectory() {
        assumeTrue(getClass().getResource("/preflight/assets/index.html") != null,
                "preflight assets are not on the classpath, the frontend was not built into the server");

        final Request request = mock(Request.class);
        // No preconditions to evaluate, so the full response gets built.
        when(request.evaluatePreconditions(any(Date.class), any(EntityTag.class))).thenReturn(null);

        final Response response = resource.index(request);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat((byte[]) response.getEntity()).isNotEmpty();
        assertThat(response.getEntityTag()).isNotNull();
    }
}
