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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.shared.rest.PermissionCoverage.Endpoint;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the permission declaration of every REST endpoint in this module to {@code target/permission-coverage.tsv}.
 * <p>
 * This test only produces a report and never fails. It builds the Jersey model of every class with {@link Path} in
 * the main classes of this module, so test resources are not listed.
 */
class PermissionCoverageReportTest {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionCoverageReportTest.class);
    private static final java.nio.file.Path REPORT = java.nio.file.Path.of("target", "permission-coverage.tsv");

    @Test
    void writeReport() {
        try {
            final List<Endpoint> endpoints = PermissionCoverage.endpoints(mainResources());
            final List<String> lines = new ArrayList<>();
            lines.add(String.join("\t", "http_method", "path", "java_method", "declaration"));
            endpoints.forEach(e -> lines.add(String.join("\t", e.httpMethod(), e.path(), e.javaMethod(), e.declarationSummary())));
            Files.createDirectories(REPORT.getParent());
            Files.write(REPORT, lines, StandardCharsets.UTF_8);

            if (endpoints.isEmpty()) {
                LOG.warn("Permission coverage report is empty: the scan of the main classes found no REST endpoints");
            }
            final long declared = endpoints.stream().filter(Endpoint::isDeclared).count();
            LOG.info("Permission coverage: {} of {} endpoints declared. Report: {}",
                    declared, endpoints.size(), REPORT.toAbsolutePath());
        } catch (Exception e) {
            LOG.warn("Could not write the permission coverage report", e);
        }
    }

    private static List<Resource> mainResources() throws URISyntaxException {
        final java.nio.file.Path mainClasses = java.nio.file.Path.of(PermissionCoverage.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        final List<Resource> resources = new ArrayList<>();
        try (final ScanResult scanResult = new ClassGraph()
                .enableClasspathEntries(mainClasses.toString())
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages("org.graylog", "org.graylog2")
                .scan()) {
            for (final ClassInfo classInfo : scanResult.getClassesWithAnnotation(Path.class)) {
                try {
                    resources.add(Resource.from(Class.forName(classInfo.getName(), false, PermissionCoverageReportTest.class.getClassLoader())));
                } catch (ClassNotFoundException | RuntimeException e) {
                    LOG.warn("Could not build the Jersey model of {}", classInfo.getName(), e);
                }
            }
        }
        return resources;
    }
}
