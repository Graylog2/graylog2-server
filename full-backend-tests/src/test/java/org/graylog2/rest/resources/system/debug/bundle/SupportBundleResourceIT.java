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
package org.graylog2.rest.resources.system.debug.bundle;

import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS)
public class SupportBundleResourceIT {
    private static final String MANIFEST_URL = "/system/debug/support/manifest";
    private static final String LOGFILE_URL = "/system/debug/support/logfile/{id}";
    private static final String BUILD_URL = "/system/debug/support/bundle/build";
    private static final String LIST_URL = "/system/debug/support/bundle/list";
    private static final String DOWNLOAD_URL = "/system/debug/support/bundle/download/{filename}";
    private static final String DELETE_URL = "/system/debug/support/bundle/{filename}";

    private static GraylogApis api;
    private static Users.User readerUser;
    private static Users.User unprivilegedUser;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) {
        api = graylogApis;

        readerUser = new Users.User("bundle.reader", "password123!", "Bundle", "Reader",
                "bundle.reader@graylog", false, 300_000, "UTC",
                List.of(), List.of(RestPermissions.SUPPORTBUNDLE_READ));
        api.users().createUser(readerUser);

        unprivilegedUser = new Users.User("bundle.noaccess", "password123!", "Bundle", "NoAccess",
                "bundle.noaccess@graylog", false, 300_000, "UTC",
                List.of(), List.of());
        api.users().createUser(unprivilegedUser);
    }

    @AfterAll
    static void tearDown() {
        api.users().deleteUser(readerUser.username());
        api.users().deleteUser(unprivilegedUser.username());
    }

    // --- Manifest tests ---

    @FullBackendTest
    void getManifestReturnsLogfiles() {
        given()
                .spec(api.requestSpecification())
                .when()
                .get(MANIFEST_URL)
                .then()
                .statusCode(200)
                .body("entries.logfiles", notNullValue());
    }

    @FullBackendTest
    void getManifestContainsInMemoryLog() {
        given()
                .spec(api.requestSpecification())
                .when()
                .get(MANIFEST_URL)
                .then()
                .statusCode(200)
                .body("entries.logfiles.find { it.id == 'memory' }.name", equalTo("server.mem.log"));
    }

    @FullBackendTest
    void getManifestRequiresReadPermission() {
        given()
                .spec(api.forUser(unprivilegedUser).requestSpecification())
                .when()
                .get(MANIFEST_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void getManifestAccessibleByReaderUser() {
        given()
                .spec(api.forUser(readerUser).requestSpecification())
                .when()
                .get(MANIFEST_URL)
                .then()
                .statusCode(200)
                .body("entries.logfiles", notNullValue());
    }

    // --- Logfile download tests ---

    @FullBackendTest
    void getInMemoryLogFileReturnsContent() {
        given()
                .spec(api.requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("id", "memory")
                .get(LOGFILE_URL)
                .then()
                .statusCode(200)
                .header("Content-Disposition", not(emptyOrNullString()));
    }

    @FullBackendTest
    void getLogFileWithUnknownIdReturns404() {
        given()
                .spec(api.requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("id", "nonexistent-log-id")
                .get(LOGFILE_URL)
                .then()
                .statusCode(404);
    }

    @FullBackendTest
    void getLogFileRequiresReadPermission() {
        given()
                .spec(api.forUser(unprivilegedUser).requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("id", "memory")
                .get(LOGFILE_URL)
                .then()
                .statusCode(403);
    }

    // --- Bundle build, list, download, delete tests ---

    @FullBackendTest
    void buildBundleReturns202() {
        given()
                .spec(api.requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(202);
    }

    @FullBackendTest
    void buildBundleRequiresCreatePermission() {
        given()
                .spec(api.forUser(readerUser).requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void buildBundleRequiresAuthentication() {
        given()
                .spec(api.forUser(unprivilegedUser).requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void listBundlesAfterBuildReturnsNonEmptyList() {
        // Build a bundle first
        given()
                .spec(api.requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(202);

        // List should contain at least one bundle
        given()
                .spec(api.requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)))
                .body("[0].file_name", not(emptyOrNullString()))
                .body("[0].size", greaterThan(0));
    }

    @FullBackendTest
    void listBundlesRequiresReadPermission() {
        given()
                .spec(api.forUser(unprivilegedUser).requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void downloadBundleReturnsZipContent() {
        // Build a bundle
        given()
                .spec(api.requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(202);

        // Get the filename from list
        final String filename = given()
                .spec(api.requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(200)
                .extract().path("[0].file_name");

        // Download it
        given()
                .spec(api.requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("filename", filename)
                .get(DOWNLOAD_URL)
                .then()
                .statusCode(200)
                .header("Content-Disposition", not(emptyOrNullString()));
    }

    @FullBackendTest
    void downloadBundleRequiresReadPermission() {
        // Build a bundle first so we have a filename to attempt
        given()
                .spec(api.requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(202);

        final String filename = given()
                .spec(api.requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(200)
                .extract().path("[0].file_name");

        given()
                .spec(api.forUser(unprivilegedUser).requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("filename", filename)
                .get(DOWNLOAD_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void downloadBundleWithPathTraversalReturns404() {
        given()
                .spec(api.requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("filename", "../etc/passwd")
                .get(DOWNLOAD_URL)
                .then()
                .statusCode(404);
    }

    @FullBackendTest
    void deleteBundleReturns202() {
        // Build a bundle to delete
        given()
                .spec(api.requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(202);

        final String filename = given()
                .spec(api.requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(200)
                .extract().path("[0].file_name");

        given()
                .spec(api.requestSpecification())
                .when()
                .pathParam("filename", filename)
                .delete(DELETE_URL)
                .then()
                .statusCode(202);

        // Verify it's gone
        given()
                .spec(api.requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(200)
                .body("collect { it.file_name }", not(org.hamcrest.Matchers.hasItem(filename)));
    }

    @FullBackendTest
    void deleteNonExistentBundleReturns404() {
        given()
                .spec(api.requestSpecification())
                .when()
                .pathParam("filename", "nonexistent-bundle.zip")
                .delete(DELETE_URL)
                .then()
                .statusCode(404);
    }

    @FullBackendTest
    void deleteBundleRequiresCreatePermission() {
        given()
                .spec(api.forUser(readerUser).requestSpecification())
                .when()
                .pathParam("filename", "any-bundle.zip")
                .delete(DELETE_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void deleteBundleWithPathTraversalReturns404() {
        given()
                .spec(api.requestSpecification())
                .when()
                .pathParam("filename", "../etc/passwd")
                .delete(DELETE_URL)
                .then()
                .statusCode(404);
    }

    @FullBackendTest
    void downloadedBundleIsValidZipWithExpectedEntries() throws Exception {
        given()
                .spec(api.requestSpecification())
                .when()
                .post(BUILD_URL)
                .then()
                .statusCode(202);

        final String filename = given()
                .spec(api.requestSpecification())
                .when()
                .get(LIST_URL)
                .then()
                .statusCode(200)
                .extract().path("[0].file_name");

        final byte[] zipBytes = given()
                .spec(api.requestSpecification())
                .accept("application/octet-stream")
                .when()
                .pathParam("filename", filename)
                .get(DOWNLOAD_URL)
                .then()
                .statusCode(200)
                .extract().asByteArray();

        final List<String> entryNames;
        try (final ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            final var entries = new java.util.ArrayList<String>();
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
                zip.closeEntry();
            }
            entryNames = entries;
        }

        assertThat(zipBytes).isNotEmpty();
        assertThat(entryNames).isNotEmpty();
        // The bundle must always contain a cluster.json summary
        assertThat(entryNames).anyMatch(name -> name.equals("cluster.json"));
        // Each node's directory should contain a thread dump and metrics
        assertThat(entryNames).anyMatch(name -> name.endsWith("thread-dump.txt"));
        assertThat(entryNames).anyMatch(name -> name.endsWith("metrics.json"));
        // At least one log file should be present (the in-memory log)
        assertThat(entryNames).anyMatch(name -> name.endsWith("server.mem.log"));
    }
}
