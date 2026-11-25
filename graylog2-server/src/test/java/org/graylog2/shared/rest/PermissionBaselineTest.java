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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.shared.rest.PermissionBaseline.Area;
import org.graylog2.shared.rest.PermissionCoverage.Endpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Fails when a REST endpoint of this module has no permission declaration and is not in the baseline, or when a
 * baseline entry has a declaration now or no longer exists.
 * <p>
 * The baseline lists the endpoints that were undeclared when this test was added. It only shrinks. The test also
 * writes the permission declaration of every endpoint to {@code target/permission-coverage.tsv}. It scans the main
 * classes of this module, so test resources are not checked.
 * <p>
 * The other tests check {@link PermissionBaseline} against the fixture resources below.
 */
class PermissionBaselineTest {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionBaselineTest.class);
    private static final Path REPORT = Path.of("target", "permission-coverage.tsv");
    private static final Path BASELINE = Path.of("src", "test", "resources", "org", "graylog2", "shared", "rest", "permission-baseline");
    private static final String FIXTURE_FILE = f("%s$Fixture.txt", PermissionBaselineTest.class.getName());

    // Each area has its own file, so that changes that declare the endpoints of different areas never edit the same
    // file. Everything else goes to remaining.txt.
    private static final List<Area> AREAS = List.of(
            Area.of("org.graylog.collectors"),
            Area.of("org.graylog.events"),
            Area.of("org.graylog.plugins.pipelineprocessor"),
            Area.of("org.graylog.plugins.views"),
            Area.of("org.graylog.security"),
            Area.of("org.graylog2.contentstream"),
            Area.of("org.graylog2.rest.resources.HelloWorldResource"),
            Area.of("org.graylog2.rest.resources.cluster"),
            Area.of("org.graylog2.rest.resources.datanodes"),
            Area.of("org.graylog2.rest.resources.messages"),
            Area.of("org.graylog2.rest.resources.search"),
            Area.of("org.graylog2.rest.resources.streams"),
            Area.of("org.graylog2.rest.resources.suggestions"),
            Area.packageOnly("org.graylog2.rest.resources.system"),
            Area.of("org.graylog2.rest.resources.system.contentpacks"),
            Area.of("org.graylog2.rest.resources.system.debug"),
            Area.of("org.graylog2.rest.resources.system.field_types"),
            Area.of("org.graylog2.rest.resources.system.indexer"),
            Area.of("org.graylog2.rest.resources.system.indices"),
            Area.of("org.graylog2.rest.resources.system.inputs"),
            Area.of("org.graylog2.rest.resources.system.jobs"),
            Area.of("org.graylog2.rest.resources.system.logs"),
            Area.of("org.graylog2.rest.resources.system.lookup"),
            Area.of("org.graylog2.rest.resources.system.outputs"),
            Area.of("org.graylog2.rest.resources.system.processing"),
            Area.of("org.graylog2.rest.resources.tools"),
            Area.of("org.graylog2.rest.resources.users"),
            Area.of("org.graylog2.shared"),
            Area.of("org.graylog2.telemetry"),
            Area.of("org.graylog2.web"));

    @Test
    void undeclaredEndpointsMatchTheBaseline() throws Exception {
        assertThat(Path.of("src", "test", "java"))
                .as("The test must run in the module directory, because the baseline path is relative to it")
                .isDirectory();
        final List<Endpoint> endpoints = PermissionBaseline.scanEndpoints(PermissionCoverage.class, "org.graylog", "org.graylog2");
        assertThat(endpoints).as("REST endpoints found by the scan of the main classes").isNotEmpty();

        PermissionBaseline.writeReport(endpoints, REPORT);
        final long declared = endpoints.stream().filter(Endpoint::isDeclared).count();
        LOG.info("Permission coverage: {} of {} endpoints declared. Report: {}", declared, endpoints.size(), REPORT.toAbsolutePath());

        new PermissionBaseline(BASELINE, AREAS).check(endpoints).ifPresent(message -> fail(message));
    }

    @Test
    void entryHasHttpMethodJavaMethodAndPath() {
        assertThat(fixtureEndpoints()).extracting(PermissionBaseline::entry).containsExactly(
                f("GET %s#get /fixture/baseline", Fixture.class.getName()),
                f("GET %s#declared /fixture/baseline/declared", Fixture.class.getName()),
                f("GET %s#get /fixture/baseline/{id}", Fixture.class.getName()),
                f("GET %s#other /fixture/other", OtherFixture.class.getName()));
    }

    @Test
    void areaWithTheLongestMatchingPrefixWins() {
        final PermissionBaseline baseline = new PermissionBaseline(Path.of("baseline"), List.of(
                Area.of("org.graylog2.shared"),
                Area.packageOnly("org.graylog2.shared.rest"),
                Area.of(f("%s$OtherFixture", PermissionBaselineTest.class.getName()))));

        assertThat(fixtureEndpoints()).extracting(baseline::fileFor).containsExactly(
                "org.graylog2.shared.rest.txt",
                "org.graylog2.shared.rest.txt",
                "org.graylog2.shared.rest.txt",
                f("%s$OtherFixture.txt", PermissionBaselineTest.class.getName()));
    }

    @Test
    void areaMatchesPackageAndClassNamesAtNameBoundaries() {
        assertThat(Area.of("org.graylog2.shared").matches(PermissionBaseline.class)).isTrue();
        assertThat(Area.of("org.graylog2.shared.rest.PermissionBaseline").matches(PermissionBaseline.class)).isTrue();
        assertThat(Area.of("org.graylog2.shared.rest.PermissionBaseline").matches(Area.class)).isTrue();
        assertThat(Area.of("org.graylog2.share").matches(PermissionBaseline.class)).isFalse();
        assertThat(Area.of("org.graylog2.shared.rest.Permission").matches(PermissionBaseline.class)).isFalse();
        assertThat(Area.packageOnly("org.graylog2.shared.rest").matches(PermissionBaseline.class)).isTrue();
        assertThat(Area.packageOnly("org.graylog2.shared").matches(PermissionBaseline.class)).isFalse();
    }

    @Test
    void endpointsWithoutAreaGoToTheRemainingFile() {
        final PermissionBaseline baseline = new PermissionBaseline(Path.of("baseline"), List.of(Area.of("org.graylog.events")));

        assertThat(baseline.expected(fixtureEndpoints())).containsOnlyKeys(PermissionBaseline.REMAINING_FILE);
    }

    @Test
    void rejectsAreasWithTheSameFile() {
        assertThatThrownBy(() -> new PermissionBaseline(Path.of("baseline"), List.of(Area.of("a.b"), Area.packageOnly("a.b"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PermissionBaseline(Path.of("baseline"), List.of(Area.of("remaining"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void passesWhenTheBaselineListsExactlyTheUndeclaredEndpoints(@TempDir Path dir) throws IOException {
        final PermissionBaseline baseline = fixtureBaseline(dir);
        writeBaseline(dir, baseline.expected(fixtureEndpoints()));

        assertThat(baseline.check(fixtureEndpoints())).isEmpty();
        assertThat(Files.readAllLines(dir.resolve(FIXTURE_FILE), StandardCharsets.UTF_8)).containsExactly(
                f("GET %s#get /fixture/baseline", Fixture.class.getName()),
                f("GET %s#get /fixture/baseline/{id}", Fixture.class.getName()));
    }

    @Test
    void ignoresBlankLinesAndSurroundingWhitespace(@TempDir Path dir) throws IOException {
        final PermissionBaseline baseline = fixtureBaseline(dir);
        final List<String> lines = baseline.expected(fixtureEndpoints()).get(FIXTURE_FILE).stream().map(line -> f("  %s ", line)).toList();
        Files.write(dir.resolve(FIXTURE_FILE), List.of("", lines.get(0), "", lines.get(1), ""), StandardCharsets.UTF_8);
        writeBaseline(dir, Map.of(PermissionBaseline.REMAINING_FILE, baseline.expected(fixtureEndpoints()).get(PermissionBaseline.REMAINING_FILE)));

        assertThat(baseline.check(fixtureEndpoints())).isEmpty();
    }

    @Test
    void listsUndeclaredEndpointsThatAreMissingWithTheirFileAndHowToDeclareThem(@TempDir Path dir) throws IOException {
        final PermissionBaseline baseline = fixtureBaseline(dir);
        writeBaseline(dir, Map.of(FIXTURE_FILE, Set.of(f("GET %s#get /fixture/baseline", Fixture.class.getName()))));

        assertThat(baseline.check(fixtureEndpoints())).hasValueSatisfying(message -> assertThat(message)
                .contains("@RequiresPermissions", "@InlinePermissionCheck", "@DelegatedPermissionCheck",
                        "@ProxiedPermissionCheck", "@NoPermissionCheckRequired", "org.graylog2.shared.rest", "Javadoc")
                .contains(f("""
                        Lines to add:
                          %s
                            GET %s#get /fixture/baseline/{id}
                          %s
                            GET %s#other /fixture/other
                        """, dir.resolve(FIXTURE_FILE), Fixture.class.getName(),
                        dir.resolve(PermissionBaseline.REMAINING_FILE), OtherFixture.class.getName()))
                .doesNotContain("Lines to remove"));
    }

    @Test
    void listsEntriesThatAreDeclaredGoneDuplicatedOrInTheWrongFile(@TempDir Path dir) throws IOException {
        final PermissionBaseline baseline = fixtureBaseline(dir);
        final String get = f("GET %s#get /fixture/baseline", Fixture.class.getName());
        final String getById = f("GET %s#get /fixture/baseline/{id}", Fixture.class.getName());
        final String declared = f("GET %s#declared /fixture/baseline/declared", Fixture.class.getName());
        final String gone = f("DELETE %s#gone /fixture/baseline", Fixture.class.getName());
        final String other = f("GET %s#other /fixture/other", OtherFixture.class.getName());
        Files.write(dir.resolve(FIXTURE_FILE), List.of(get, getById, declared, gone, get, other), StandardCharsets.UTF_8);
        Files.write(dir.resolve("misspelled.txt"), List.of(), StandardCharsets.UTF_8);
        Files.write(dir.resolve(".DS_Store"), List.of("binary"), StandardCharsets.UTF_8);
        Files.write(dir.resolve(f("%s~", FIXTURE_FILE)), List.of(gone), StandardCharsets.UTF_8);

        assertThat(baseline.check(fixtureEndpoints())).hasValueSatisfying(message -> assertThat(message)
                .contains(f("""
                        Lines to add:
                          %s
                            %s
                        """, dir.resolve(PermissionBaseline.REMAINING_FILE), other))
                .contains(f("""
                        Lines to remove:
                          %s
                            %s
                            %s
                            %s
                            %s
                        """, dir.resolve(FIXTURE_FILE), declared, gone, get, other))
                .contains(f("These files belong to no area. Delete them:%n  %s%n", dir.resolve("misspelled.txt")))
                .doesNotContain(".DS_Store", "binary", f("%s~", FIXTURE_FILE)));
    }

    @Test
    void missingDirectoryMeansAnEmptyBaseline(@TempDir Path dir) {
        final PermissionBaseline baseline = fixtureBaseline(dir.resolve("missing"));

        assertThat(baseline.check(fixtureEndpoints())).isPresent();
        assertThat(baseline.check(fixtureEndpoints().stream().filter(Endpoint::isDeclared).toList())).isEmpty();
    }

    private static PermissionBaseline fixtureBaseline(Path dir) {
        return new PermissionBaseline(dir, List.of(Area.of(f("%s$Fixture", PermissionBaselineTest.class.getName()))));
    }

    private static List<Endpoint> fixtureEndpoints() {
        return PermissionCoverage.endpoints(List.of(Resource.from(Fixture.class), Resource.from(OtherFixture.class)));
    }

    private static void writeBaseline(Path dir, Map<String, Set<String>> files) throws IOException {
        for (final Map.Entry<String, Set<String>> file : files.entrySet()) {
            Files.write(dir.resolve(file.getKey()), file.getValue(), StandardCharsets.UTF_8);
        }
    }

    @jakarta.ws.rs.Path("/fixture/baseline")
    public static class Fixture {
        @GET
        public String get() {
            return "";
        }

        @GET
        @jakarta.ws.rs.Path("{id}")
        public String get(@PathParam("id") String id) {
            return id;
        }

        @GET
        @jakarta.ws.rs.Path("declared")
        @RequiresPermissions("fixture:read")
        public String declared() {
            return "";
        }
    }

    @jakarta.ws.rs.Path("/fixture/other")
    public static class OtherFixture {
        @GET
        public String other() {
            return "";
        }
    }
}
