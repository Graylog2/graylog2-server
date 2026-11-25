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
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.shared.rest.PermissionCoverage.Endpoint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Compares the undeclared REST endpoints of a module with a baseline of endpoints that are allowed to stay undeclared.
 * <p>
 * The baseline is a directory with one file per {@link Area} and one {@value #REMAINING_FILE} for the endpoints that
 * belong to no area. Each line of a file is the {@link #entry(Endpoint) entry} of one undeclared endpoint. A missing
 * file counts as empty. The baseline only shrinks: {@link #check(List)} reports an undeclared endpoint that is missing
 * from its file, and an entry that belongs to no undeclared endpoint of that file's area.
 * <p>
 * The areas follow the chunks that declare the endpoints, so that two chunks never edit the same file. An endpoint
 * belongs to the area with the longest prefix that matches its resource class.
 * <p>
 * This class is shared with other modules through the test-jar. It reads only Jersey models and files.
 */
public final class PermissionBaseline {
    public static final String REMAINING_FILE = "remaining.txt";
    private static final String REPORT_HEADER = String.join("\t", "http_method", "path", "java_method", "declaration");

    private final Path directory;
    private final List<Area> areas;

    /**
     * @param directory the baseline directory, relative to the module directory in which the test runs
     * @param areas     the areas, each with its own file
     */
    public PermissionBaseline(Path directory, List<Area> areas) {
        final Set<String> files = new HashSet<>();
        for (final Area area : areas) {
            if (!files.add(area.fileName()) || area.fileName().equals(REMAINING_FILE)) {
                throw new IllegalArgumentException(f("Baseline file %s is used twice or is reserved", area.fileName()));
            }
        }
        this.directory = directory;
        this.areas = List.copyOf(areas);
    }

    /**
     * The baseline line of an endpoint: HTTP method, resource class and Java method, and path, separated by single
     * spaces. The path is needed because overloaded Java methods can share the HTTP method.
     * <p>
     * Example: {@code GET org.graylog2.rest.resources.streams.StreamResource#get /streams/{streamId}}
     */
    public static String entry(Endpoint endpoint) {
        return f("%s %s %s", endpoint.httpMethod(), endpoint.javaMethod(), endpoint.path());
    }

    /**
     * Returns the endpoints of every class with {@code @Path} in the classes directory or jar that contains
     * {@code anchor}. Test classes are not scanned when {@code anchor} is a main class.
     *
     * @param anchor   a class in the main classes of the module
     * @param packages the packages to scan, with their subpackages
     */
    public static List<Endpoint> scanEndpoints(Class<?> anchor, String... packages) {
        final Path classes;
        try {
            classes = Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(f("Cannot locate the classes of %s", anchor.getName()), e);
        }
        final List<Resource> resources = new ArrayList<>();
        // enableClasspathEntries is required: without it the scan of the given path finds nothing.
        try (final ScanResult scanResult = new ClassGraph()
                .enableClasspathEntries(classes.toString())
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(packages)
                .scan()) {
            for (final ClassInfo classInfo : scanResult.getClassesWithAnnotation(jakarta.ws.rs.Path.class)) {
                try {
                    resources.add(Resource.from(Class.forName(classInfo.getName(), false, anchor.getClassLoader())));
                } catch (ClassNotFoundException | RuntimeException e) {
                    throw new IllegalStateException(f("Cannot build the Jersey model of %s", classInfo.getName()), e);
                }
            }
        }
        return PermissionCoverage.endpoints(resources);
    }

    /**
     * Writes one row per endpoint with HTTP method, path, Java method and declarations as tab-separated values.
     */
    public static void writeReport(List<Endpoint> endpoints, Path report) throws IOException {
        final List<String> lines = new ArrayList<>();
        lines.add(REPORT_HEADER);
        endpoints.forEach(e -> lines.add(String.join("\t", e.httpMethod(), e.path(), e.javaMethod(), e.declarationSummary())));
        if (report.getParent() != null) {
            Files.createDirectories(report.getParent());
        }
        Files.write(report, lines, StandardCharsets.UTF_8);
    }

    /**
     * The name of the baseline file for an endpoint: the file of the area with the longest matching prefix, or
     * {@value #REMAINING_FILE}.
     */
    public String fileFor(Endpoint endpoint) {
        final Class<?> resourceClass = endpoint.resourceClass();
        return areas.stream()
                .filter(area -> area.matches(resourceClass))
                .max(Comparator.comparingInt(area -> area.prefix().length()))
                .map(Area::fileName)
                .orElse(REMAINING_FILE);
    }

    /**
     * The baseline that the given endpoints need: the entries of the undeclared endpoints, by file name.
     */
    public Map<String, Set<String>> expected(List<Endpoint> endpoints) {
        return endpoints.stream()
                .filter(endpoint -> !endpoint.isDeclared())
                .collect(Collectors.groupingBy(this::fileFor, TreeMap::new,
                        Collectors.mapping(PermissionBaseline::entry, Collectors.toCollection(TreeSet::new))));
    }

    /**
     * Compares the undeclared endpoints with the baseline files.
     *
     * @return a message that lists the lines to add and remove, file by file, or empty if the baseline matches
     */
    public Optional<String> check(List<Endpoint> endpoints) {
        final Map<String, Set<String>> expected = expected(endpoints);
        final Map<String, List<String>> actual = readFiles();

        final Map<String, Set<String>> toAdd = new TreeMap<>();
        final Map<String, List<String>> toRemove = new TreeMap<>();
        final Set<String> files = new TreeSet<>(expected.keySet());
        files.addAll(actual.keySet());
        for (final String file : files) {
            final Set<String> wanted = expected.getOrDefault(file, Set.of());
            final List<String> lines = actual.getOrDefault(file, List.of());
            final Set<String> missing = new TreeSet<>(wanted);
            lines.forEach(missing::remove);
            if (!missing.isEmpty()) {
                toAdd.put(file, missing);
            }
            final Set<String> seen = new HashSet<>();
            final List<String> stale = lines.stream().filter(line -> !wanted.contains(line) || !seen.add(line)).toList();
            if (!stale.isEmpty()) {
                toRemove.put(file, stale);
            }
        }
        final Set<String> unknownFiles = new TreeSet<>(actual.keySet());
        unknownFiles.removeAll(knownFiles());

        if (toAdd.isEmpty() && toRemove.isEmpty() && unknownFiles.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(message(toAdd, toRemove, unknownFiles));
    }

    private Set<String> knownFiles() {
        final Set<String> known = areas.stream().map(Area::fileName).collect(Collectors.toCollection(HashSet::new));
        known.add(REMAINING_FILE);
        return known;
    }

    // Every .txt file in the directory, so that a misspelled file name is reported. Hidden files such as .DS_Store
    // and editor backups are not baseline files. Blank lines are ignored.
    private Map<String, List<String>> readFiles() {
        final Map<String, List<String>> files = new TreeMap<>();
        if (!Files.isDirectory(directory)) {
            return files;
        }
        try (final Stream<Path> paths = Files.list(directory)) {
            for (final Path path : paths.filter(PermissionBaseline::isBaselineFile).toList()) {
                final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty())
                        .toList();
                files.put(path.getFileName().toString(), lines);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(f("Cannot read the permission baseline in %s", directory), e);
        }
        return files;
    }

    private static boolean isBaselineFile(Path path) {
        final String name = path.getFileName().toString();
        return Files.isRegularFile(path) && name.endsWith(".txt") && !name.startsWith(".");
    }

    private String message(Map<String, Set<String>> toAdd, Map<String, List<String>> toRemove, Set<String> unknownFiles) {
        final StringBuilder message = new StringBuilder(f("The permission baseline in %s does not match the REST endpoints.%n", directory));
        if (!toAdd.isEmpty()) {
            message.append("""

                    These endpoints have no permission declaration and are not in the baseline.

                    If you added the endpoint, declare its permission handling instead of adding it to the baseline:
                    annotate the method with @RequiresPermissions, or with one of @InlinePermissionCheck,
                    @DelegatedPermissionCheck, @ProxiedPermissionCheck or @NoPermissionCheckRequired from
                    org.graylog2.shared.rest. The Javadoc of these four annotations explains when each one applies.

                    If the endpoint was already in the baseline and you only renamed or moved it, or changed its path,
                    replace its old line with the new one. The old line is listed under the lines to remove.
                    """);
            appendLines(message, "Lines to add", toAdd);
        }
        if (!toRemove.isEmpty()) {
            message.append("""

                    These baseline entries belong to no undeclared endpoint of their area: the endpoint has a
                    permission declaration now, it no longer exists, or the line is a duplicate. The baseline only
                    shrinks, so remove the lines.
                    """);
            appendLines(message, "Lines to remove", toRemove);
        }
        if (!unknownFiles.isEmpty()) {
            message.append(f("%nThese files belong to no area. Delete them:%n"));
            unknownFiles.forEach(file -> message.append(f("  %s%n", directory.resolve(file))));
        }
        return message.toString();
    }

    private void appendLines(StringBuilder message, String title, Map<String, ? extends Iterable<String>> linesByFile) {
        message.append(f("%n%s:%n", title));
        linesByFile.forEach((file, lines) -> {
            message.append(f("  %s%n", directory.resolve(file)));
            lines.forEach(line -> message.append(f("    %s%n", line)));
        });
    }

    /**
     * A group of resource classes with its own baseline file. The file name is the prefix followed by {@code .txt}.
     *
     * @param prefix             a package name or a fully qualified class name
     * @param includeSubpackages {@code true} if the area also contains the subpackages of the prefix and, if the
     *                           prefix is a class name, the class and its nested classes; {@code false} if it
     *                           contains only the classes of the package itself
     */
    public record Area(String prefix, boolean includeSubpackages) {
        /**
         * The package or class {@code prefix} and everything below it.
         */
        public static Area of(String prefix) {
            return new Area(prefix, true);
        }

        /**
         * The classes of the package {@code packageName}, without its subpackages.
         */
        public static Area packageOnly(String packageName) {
            return new Area(packageName, false);
        }

        public String fileName() {
            return f("%s.txt", prefix);
        }

        boolean matches(Class<?> resourceClass) {
            if (!includeSubpackages) {
                return resourceClass.getPackageName().equals(prefix);
            }
            final String name = resourceClass.getName();
            return name.equals(prefix) || name.startsWith(f("%s.", prefix)) || name.startsWith(f("%s$", prefix));
        }
    }
}
