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
package org.graylog2.shared.rest;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.assertj.core.groups.Tuple;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.shared.rest.PermissionCoverage.Declaration;
import org.graylog2.shared.rest.PermissionCoverage.Endpoint;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.graylog2.shared.utilities.StringUtils.f;

class PermissionCoverageTest {

    @Test
    void listsOneRowPerEndpointWithPathAndJavaMethod() {
        final List<Endpoint> endpoints = endpointsOf(DeclaredFixture.class);

        assertThat(endpoints)
                .extracting(Endpoint::httpMethod, Endpoint::path, Endpoint::javaMethod)
                .containsExactly(
                        row("GET", "/fixture/declared/delegated", DeclaredFixture.class, "delegated"),
                        row("GET", "/fixture/declared/guest", DeclaredFixture.class, "guest"),
                        row("GET", "/fixture/declared/inline", DeclaredFixture.class, "inline"),
                        row("GET", "/fixture/declared/none", DeclaredFixture.class, "none"),
                        row("GET", "/fixture/declared/proxied", DeclaredFixture.class, "proxied"),
                        row("GET", "/fixture/declared/shiro", DeclaredFixture.class, "shiro"),
                        row("DELETE", "/fixture/declared/{id}", DeclaredFixture.class, "undeclared"));
    }

    @Test
    void recognizesEveryDeclaration() {
        final List<Endpoint> endpoints = endpointsOf(DeclaredFixture.class);

        assertThat(declarationTypes(endpoint(endpoints, "shiro"))).containsExactly(RequiresPermissions.class);
        assertThat(declarationTypes(endpoint(endpoints, "guest"))).containsExactly(RequiresGuest.class);
        assertThat(declarationTypes(endpoint(endpoints, "inline"))).containsExactly(InlinePermissionCheck.class);
        assertThat(declarationTypes(endpoint(endpoints, "delegated"))).containsExactly(DelegatedPermissionCheck.class);
        assertThat(declarationTypes(endpoint(endpoints, "proxied"))).containsExactly(ProxiedPermissionCheck.class);
        assertThat(declarationTypes(endpoint(endpoints, "none"))).containsExactly(NoPermissionCheckRequired.class);
        assertThat(endpoints).allMatch(e -> e.isDeclared() == !e.method().getName().equals("undeclared"));
    }

    @Test
    void keepsTheAnnotationAttributes() {
        final Endpoint none = endpoint(endpointsOf(DeclaredFixture.class), "none");

        assertThat(none.declarations()).singleElement().satisfies(declaration -> {
            assertThat(declaration.classLevel()).isFalse();
            assertThat(((NoPermissionCheckRequired) declaration.annotation()).value()).isEqualTo(Exemption.REQUEST_INPUT_ONLY);
        });
    }

    @Test
    void reportsAnUndeclaredEndpoint() {
        final Endpoint undeclared = endpoint(endpointsOf(DeclaredFixture.class), "undeclared");

        assertThat(undeclared.isDeclared()).isFalse();
        assertThat(undeclared.declarations()).isEmpty();
        assertThat(undeclared.declarationSummary()).isEqualTo("none");
    }

    @Test
    void appliesClassLevelRequiresPermissionsToEveryMethod() {
        final List<Endpoint> endpoints = endpointsOf(ClassPermissionsFixture.class);

        assertThat(endpoints).hasSize(2).allSatisfy(endpoint -> assertThat(endpoint.isDeclared()).isTrue());
        assertThat(endpoint(endpoints, "list").declarationSummary()).isEqualTo("RequiresPermissions(class)");
        assertThat(endpoint(endpoints, "create").declarationSummary())
                .isEqualTo("RequiresPermissions(class)+NoPermissionCheckRequired");
    }

    @Test
    void ignoresClassLevelRequiresGuest() {
        final List<Endpoint> endpoints = endpointsOf(ClassGuestFixture.class);

        assertThat(endpoints).singleElement().satisfies(endpoint -> assertThat(endpoint.isDeclared()).isFalse());
    }

    @Test
    void reportsEveryDeclarationOfAMethod() {
        final Endpoint both = endpoint(endpointsOf(DoubleFixture.class), "both");

        assertThat(declarationTypes(both)).containsExactly(RequiresPermissions.class, InlinePermissionCheck.class);
        assertThat(both.declarationSummary()).isEqualTo("RequiresPermissions+InlinePermissionCheck");
    }

    @Test
    void skipsSubResourceLocators() {
        final List<Endpoint> endpoints = endpointsOf(LocatorFixture.class);

        assertThat(endpoints).extracting(Endpoint::path).containsExactly("/fixture/locator");
    }

    @Test
    void sortsRowsAcrossResources() {
        final List<Endpoint> endpoints = PermissionCoverage.endpoints(List.of(
                Resource.from(LocatorFixture.class), Resource.from(ClassGuestFixture.class)));

        assertThat(endpoints).extracting(Endpoint::path).containsExactly("/fixture/guest", "/fixture/locator");
    }

    @Test
    void normalizesSlashesInPaths() {
        final List<Endpoint> endpoints = endpointsOf(SlashesFixture.class);

        assertThat(endpoints).extracting(Endpoint::path).containsExactly("/fixture/slashes/item");
    }

    private static List<Endpoint> endpointsOf(Class<?> resourceClass) {
        return PermissionCoverage.endpoints(List.of(Resource.from(resourceClass)));
    }

    private static Endpoint endpoint(List<Endpoint> endpoints, String methodName) {
        return endpoints.stream()
                .filter(e -> e.method().getName().equals(methodName))
                .findFirst()
                .orElseThrow();
    }

    private static List<Class<? extends Annotation>> declarationTypes(Endpoint endpoint) {
        return endpoint.declarations().stream().<Class<? extends Annotation>>map(Declaration::type).toList();
    }

    private static Tuple row(String httpMethod, String path, Class<?> resourceClass, String method) {
        return tuple(httpMethod, path, f("%s#%s", resourceClass.getName(), method));
    }

    @Path("/fixture/declared")
    public static class DeclaredFixture {
        @GET
        @Path("shiro")
        @RequiresPermissions("fixture:read")
        public String shiro() {
            return "";
        }

        @GET
        @Path("guest")
        @RequiresGuest
        public String guest() {
            return "";
        }

        @GET
        @Path("inline")
        @InlinePermissionCheck(value = "fixture:read", entityParam = "id")
        public String inline() {
            return "";
        }

        @GET
        @Path("delegated")
        @DelegatedPermissionCheck(to = Object.class, reason = "A job checks the permissions of the caller.")
        public String delegated() {
            return "";
        }

        @GET
        @Path("proxied")
        @ProxiedPermissionCheck(target = ClassPermissionsFixture.class, method = "list")
        public String proxied() {
            return "";
        }

        @GET
        @Path("none")
        @NoPermissionCheckRequired(value = Exemption.REQUEST_INPUT_ONLY, reason = "Echoes the request.")
        public String none() {
            return "";
        }

        @DELETE
        @Path("{id}")
        public void undeclared(@PathParam("id") String id) {
        }
    }

    @Path("/fixture/class")
    @RequiresPermissions("fixture:read")
    public static class ClassPermissionsFixture {
        @GET
        public String list() {
            return "";
        }

        @POST
        @NoPermissionCheckRequired(value = Exemption.ANY_AUTHENTICATED_USER, reason = "Returns static data.")
        public String create() {
            return "";
        }
    }

    @Path("/fixture/guest")
    @RequiresGuest
    public static class ClassGuestFixture {
        @GET
        public String get() {
            return "";
        }
    }

    @Path("/fixture/double")
    public static class DoubleFixture {
        @PUT
        @RequiresPermissions("fixture:edit")
        @InlinePermissionCheck("fixture:edit")
        public String both() {
            return "";
        }
    }

    @Path("/fixture/locator")
    public static class LocatorFixture {
        @GET
        public String get() {
            return "";
        }

        @Path("sub")
        public ClassGuestFixture locator() {
            return new ClassGuestFixture();
        }
    }

    @Path("fixture/slashes/")
    public static class SlashesFixture {
        @GET
        @Path("/item")
        public String get() {
            return "";
        }
    }
}
