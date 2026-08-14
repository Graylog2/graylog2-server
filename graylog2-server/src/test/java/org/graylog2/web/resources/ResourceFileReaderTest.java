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
package org.graylog2.web.resources;

import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ResourceFileReaderTest {
    private static final String TEST_ASSETS_DIR = "/org/graylog2/web/resources/assets-test/";

    private final ResourceFileReader reader = new ResourceFileReader();

    @ParameterizedTest
    @ValueSource(strings = {"index.html", "abc.js", "abc.js.map", "./abc.js", "abc.js/"})
    void acceptsAndCanonicalizesPlainFileNames(String filename) throws Exception {
        assertThat(ResourceFileReader.resolveResourceName(ResourceFileReader.WEB_INTERFACE_ASSETS_DIR, filename))
                .isEqualTo("/web-interface/assets/" + Paths.get(filename).getFileName());
    }

    @Test
    void acceptsPlainFileNamesInThePluginJarRoot() throws Exception {
        assertThat(ResourceFileReader.resolveResourceName(ResourceFileReader.PLUGIN_ASSETS_DIR, "abc.js"))
                .isEqualTo("/abc.js");
    }

    @Test
    void appendsASeparatorToABaseDirectoryWithoutATrailingSlash() throws Exception {
        assertThat(ResourceFileReader.resolveResourceName("/web-interface/assets", "abc.js"))
                .isEqualTo("/web-interface/assets/abc.js");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "..",
            "../",
            "./../abc.js",
            "foo/../../..",
            "/etc/passwd",
            "",
            ".",
            "sub/dir/app.js",
    })
    void rejectsAnythingButADirectChildOfTheAssetsDirectory(String filename) {
        assertThatThrownBy(() -> ResourceFileReader.resolveResourceName(ResourceFileReader.WEB_INTERFACE_ASSETS_DIR, filename))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("Invalid resource file name.");
    }

    /**
     * Regression test for the plugin asset directory being the JAR root: a plain containment check
     * ({@code resolved.startsWith(base)}) passes for every one of these, because {@code "/"} contains any
     * path. Only the direct-child check rejects them.
     */
    @ParameterizedTest
    @ValueSource(strings = {"../etc/passwd", "..", "../..", "etc/passwd", "/etc/passwd", ""})
    void rejectsTraversalOutOfThePluginJarRoot(String filename) {
        assertThatThrownBy(() -> ResourceFileReader.resolveResourceName(ResourceFileReader.PLUGIN_ASSETS_DIR, filename))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("Invalid resource file name.");
    }

    @Test
    void rejectsFileNamesThatAreNotValidPaths() {
        // A NUL character makes Paths.get() throw an InvalidPathException.
        final String filenameWithNulCharacter = "abc" + (char) 0 + ".js";

        assertThatThrownBy(() -> ResourceFileReader.resolveResourceName(ResourceFileReader.WEB_INTERFACE_ASSETS_DIR, filenameWithNulCharacter))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("Invalid resource file name.");
    }

    /**
     * A backslash is a path separator on Windows but an ordinary filename character elsewhere, so the two
     * platforms reject this by different routes (rejected outright vs. resolved to a nonexistent file inside the directory).
     * Either way it must not address anything outside of the assets directory.
     */
    @Test
    void doesNotLetBackslashSeparatorsEscapeTheAssetsDirectory() {
        try {
            final String resourceName =
                    ResourceFileReader.resolveResourceName(ResourceFileReader.WEB_INTERFACE_ASSETS_DIR, "..\\abc.js");
            assertThat(resourceName).startsWith(ResourceFileReader.WEB_INTERFACE_ASSETS_DIR);
        } catch (FileNotFoundException e) {
            assertThat(e).hasMessage("Invalid resource file name.");
        }
    }

    @Test
    void readsAFileFromTheGivenDirectory() throws Exception {
        final var resource = reader.readFileFrom(TEST_ASSETS_DIR, "inside.txt", getClass());

        assertThat(new String(resource.contents().get(), StandardCharsets.UTF_8))
                .isEqualTo("inside-the-assets-directory\n");
    }

    /**
     * The traversal target exists on the classpath, so this fails only because the guard rejects it.
     */
    @Test
    void refusesToReadAnExistingFileOutsideOfTheGivenDirectory() {
        assumeTrue(getClass().getResource("/org/graylog2/web/resources/outside.txt") != null,
                "test fixture missing");

        assertThatThrownBy(() -> reader.readFileFrom(TEST_ASSETS_DIR, "../outside.txt", getClass()))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void readsAPluginAssetFromThePluginsOwnCodeSource() throws Exception {
        // This test class and the fixture both live in target/test-classes, i.e. the same code source.
        final var resource = reader.readFileFromPlugin("plugin-asset-test.js", getClass());

        assertThat(new String(resource.contents().get(), StandardCharsets.UTF_8))
                .startsWith("// Stands in for a plugin asset");
    }

    /**
     * A traversal sequence normalizes away at the JAR root, so {@code ../../log4j2.xml} resolves to the
     * perfectly root-level {@code /log4j2.xml}. Only the code source check stops it: {@code log4j2.xml} is
     * packaged in the server's classes directory, not in this "plugin's" own.
     */
    @ParameterizedTest
    @ValueSource(strings = {"log4j2.xml", "../log4j2.xml", "../../log4j2.xml", "git.properties"})
    void refusesToServePluginAssetsFromAnotherCodeSource(String filename) {
        assumeTrue(getClass().getResource("/log4j2.xml") != null, "test fixture missing");

        assertThatThrownBy(() -> reader.readFileFromPlugin(filename, getClass()))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void treatsAResourceWithoutAResolvableCodeSourceAsNotFound() {
        assertThat(ResourceFileReader.isFromCodeSourceOf(null, getClass())).isFalse();
    }

    @Test
    void doesNotLeakTheRequestedFileNameInTheNotFoundMessage() {
        assertThatThrownBy(() -> reader.readFileFrom(TEST_ASSETS_DIR, "does-not-exist.txt", getClass()))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageNotContaining("does-not-exist.txt");
    }

    /**
     * The direct-child check in {@link ResourceFileReader#resolveResourceName(String, String)} means assets in
     * a subdirectory could not be served. Both asset directories are flat today; this fails the build if a
     * future frontend build starts emitting nested assets, rather than letting them 404 in production.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/web-interface/assets", PreflightConstants.ASSETS_RESOURCE_DIR})
    void assetDirectoriesAreFlat(String assetsDir) throws Exception {
        final String directoryName = assetsDir.endsWith("/") ? assetsDir.substring(0, assetsDir.length() - 1) : assetsDir;
        final URL url = getClass().getResource(directoryName);
        assumeTrue(url != null, "assets are not on the classpath, the frontend was not built into the server");
        assumeTrue("file".equals(url.getProtocol()), "assets are not served from a directory");

        final Path root = Paths.get(url.toURI());
        try (Stream<Path> entries = Files.walk(root)) {
            assertThat(entries.filter(path -> !path.equals(root)))
                    .allSatisfy(path -> assertThat(path.getParent())
                            .as("asset %s is not a direct child of %s", path, assetsDir)
                            .isEqualTo(root));
        }
    }
}
