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
package org.graylog2.shared.rest.documentation.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomReaderOperationIdTest {

    @Test
    void corePackageGetsNoPrefix() {
        assertThat(CustomReader.derivePluginPrefix("org.graylog2.rest.resources.streams")).isEmpty();
        assertThat(CustomReader.derivePluginPrefix("org.graylog2.shared.rest")).isEmpty();
    }

    @Test
    void pluginPackageGetsCapitalizedFirstSegmentAfterPlugins() {
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins.sidecar.rest.resources"))
                .isEqualTo("Sidecar");
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins.views.search.rest"))
                .isEqualTo("Views");
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins.securityapp.investigations.rest"))
                .isEqualTo("Securityapp");
    }

    @Test
    void packageWithoutPluginsSegmentGetsNoPrefix() {
        assertThat(CustomReader.derivePluginPrefix("com.example.something")).isEmpty();
    }

    @Test
    void packageEndingInPluginsGetsNoPrefix() {
        // "plugins" is the last segment, no segment follows
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins")).isEmpty();
    }

    @Test
    void stripsTrailingResourceFromClassName() {
        assertThat(CustomReader.stripResourceSuffix("StreamResource")).isEqualTo("Stream");
        assertThat(CustomReader.stripResourceSuffix("ConfigurationResource")).isEqualTo("Configuration");
    }

    @Test
    void leavesClassNamesNotEndingInResourceUntouched() {
        assertThat(CustomReader.stripResourceSuffix("TestResource2")).isEqualTo("TestResource2");
        assertThat(CustomReader.stripResourceSuffix("Foo")).isEqualTo("Foo");
        assertThat(CustomReader.stripResourceSuffix("Resources")).isEqualTo("Resources");
    }

    @Test
    void stripsResourceSuffixOnlyFromTheEnd() {
        // "Resource" appearing in the middle isn't stripped
        assertThat(CustomReader.stripResourceSuffix("ResourceFoo")).isEqualTo("ResourceFoo");
    }

    @Test
    void normalizesCamelCaseParam() {
        assertThat(CustomReader.normalizeParamName("streamId")).isEqualTo("StreamId");
        assertThat(CustomReader.normalizeParamName("name")).isEqualTo("Name");
    }

    @Test
    void normalizesSnakeCaseParam() {
        assertThat(CustomReader.normalizeParamName("index_set_id")).isEqualTo("IndexSetId");
        assertThat(CustomReader.normalizeParamName("per_page")).isEqualTo("PerPage");
    }

    @Test
    void preservesAlreadyCapitalizedParam() {
        assertThat(CustomReader.normalizeParamName("StreamId")).isEqualTo("StreamId");
    }

    @Test
    void normalizesParamWithHyphens() {
        assertThat(CustomReader.normalizeParamName("content-type")).isEqualTo("ContentType");
    }

    @Test
    void normalizesParamWithSpaces() {
        assertThat(CustomReader.normalizeParamName(" foo bar ")).isEqualTo("FooBar");
    }

    @Test
    void doesNotLowercaseInternalCaps() {
        // XMLContent must not become Xmlcontent
        assertThat(CustomReader.normalizeParamName("XMLContent")).isEqualTo("XMLContent");
    }

    @Test
    void buildsEmptySuffixForNoParams() {
        assertThat(CustomReader.paramSuffix(List.of())).isEmpty();
        assertThat(CustomReader.paramSuffix(null)).isEmpty();
    }

    @Test
    void buildsBySuffixForOneParam() {
        assertThat(CustomReader.paramSuffix(List.of("streamId"))).isEqualTo("ByStreamId");
    }

    @Test
    void buildsByAndSuffixForMultipleParams() {
        assertThat(CustomReader.paramSuffix(List.of("streamId", "page", "per_page")))
                .isEqualTo("ByStreamIdAndPageAndPerPage");
    }

    @Test
    void buildsBySuffixUsingActualParameterOrder() {
        // The param order from the JAX-RS method is preserved, not sorted
        assertThat(CustomReader.paramSuffix(List.of("zebra", "alpha")))
                .isEqualTo("ByZebraAndAlpha");
    }

    @Test
    void derivesOperationIdFromDeclaringClassMethodAndParams() throws Exception {
        final var method = WidgetResource.class.getMethod("getWidget", String.class);
        assertThat(CustomReader.operationId(method))
                .isEqualTo("Widget_getWidgetByWidgetId");
    }

    @Test
    void readAssignsDerivedOperationIds() {
        // End-to-end through Reader.read()/parseMethod, not just the static helpers
        final var reader = new CustomReader(Map.of(), new SwaggerConfiguration());
        final var openAPI = reader.read(Set.of(WidgetResource.class), Map.of());

        assertThat(openAPI.getPaths().get("/widgets/{widgetId}")).isNotNull();
        assertThat(openAPI.getPaths().get("/widgets/{widgetId}").getGet().getOperationId())
                .isEqualTo("Widget_getWidgetByWidgetId");
    }

    @Test
    void extractsJaxRsParamNamesInDeclarationOrderIgnoringBodyParams() throws Exception {
        final var method = WidgetResource.class.getMethod(
                "searchWidgets", String.class, String.class, int.class, String.class);
        // The body parameter contributes no name, the others keep declaration order
        assertThat(CustomReader.extractParamNames(method))
                .containsExactly("widgetId", "page", "X-Request-Id");
    }

    @Test
    void explicitOperationIdAnnotationWinsOverDerivedId() {
        final var reader = new CustomReader(Map.of(), new SwaggerConfiguration());
        final var openAPI = reader.read(Set.of(WidgetResource.class), Map.of());

        assertThat(openAPI.getPaths().get("/widgets")).isNotNull();
        assertThat(openAPI.getPaths().get("/widgets").getPost().getOperationId())
                .isEqualTo("legacyCreateWidget");
    }

    @Test
    void explicitOperationIdInheritedFromSuperclassMethodWins() {
        // The @Operation annotation lives on the abstract base method, not on the override.
        // It must still win over the derived id — this requires the ReflectionUtils-based
        // annotation lookup; a plain method.getAnnotation() would miss it.
        final var reader = new CustomReader(Map.of(), new SwaggerConfiguration());
        final var openAPI = reader.read(Set.of(AnnotatedWidgetResource.class), Map.of());

        assertThat(openAPI.getPaths().get("/annotated-widgets")).isNotNull();
        assertThat(openAPI.getPaths().get("/annotated-widgets").getGet().getOperationId())
                .isEqualTo("annotatedListWidgets");
    }

    @Path("/widgets")
    @SuppressWarnings("unused")
    public static class WidgetResource {
        @GET
        @Path("/{widgetId}")
        public String getWidget(@PathParam("widgetId") String widgetId) {
            return "";
        }

        @POST
        @NoAuditEvent("Test")
        @Operation(operationId = "legacyCreateWidget")
        public String createWidget(String body) {
            return "";
        }

        // No HTTP verb annotation, so this is invisible to the Reader; it only serves
        // the extractParamNames() test.
        public String searchWidgets(@PathParam("widgetId") String widgetId,
                                    String body,
                                    @QueryParam("page") int page,
                                    @HeaderParam("X-Request-Id") String requestId) {
            return "";
        }
    }

    public abstract static class AbstractWidgetResource {
        @GET
        @SuppressWarnings("unused")
        public String listWidgets(@QueryParam("page") int page) {
            return "";
        }
    }

    public abstract static class AbstractAnnotatedWidgetResource {
        @Operation(operationId = "annotatedListWidgets")
        public abstract String listAnnotated();
    }

    @Path("/annotated-widgets")
    public static class AnnotatedWidgetResource extends AbstractAnnotatedWidgetResource {
        @GET
        @Override
        public String listAnnotated() {
            return "";
        }
    }

    @Path("/foo-widgets")
    public static class FooWidgetResource extends AbstractWidgetResource {
    }

    @Path("/bar-widgets")
    public static class BarWidgetResource extends AbstractWidgetResource {
    }
}
