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
package org.graylog.datanode.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Replaces log4j jars of a given version with newer versions across all unpacked OpenSearch distributions.
 * Run after plugin installation so newly installed plugin jars are also covered.
 */
public class ReplaceLog4jJars {

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Syntax: " + ReplaceLog4jJars.class.getSimpleName()
                    + " <opensearchParentDir> <replacementDir> <oldVersion> <newVersion>");
            System.exit(1);
        }

        final Path opensearchParentDir = Path.of(args[0]);
        final Path replacementDir = Path.of(args[1]);
        final String oldVersion = args[2];
        final String newVersion = args[3];

        try (Stream<Path> files = Files.walk(opensearchParentDir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> {
                        final String name = p.getFileName().toString();
                        return name.startsWith("log4j-") && name.endsWith("-" + oldVersion + ".jar");
                    })
                    .forEach(oldJar -> replaceJar(oldJar, replacementDir, oldVersion, newVersion));
        }
    }

    private static void replaceJar(Path oldJar, Path replacementDir, String oldVersion, String newVersion) {
        final String oldName = oldJar.getFileName().toString();
        final String newName = oldName.replace("-" + oldVersion + ".jar", "-" + newVersion + ".jar");
        final Path newJar = replacementDir.resolve(newName);

        if (!Files.exists(newJar)) {
            System.err.println("WARNING: Replacement jar not found, skipping: " + newJar);
            return;
        }

        try {
            System.out.println("Replacing " + oldJar + " -> " + newName);
            Files.copy(newJar, oldJar.resolveSibling(newName), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(oldJar);
        } catch (IOException e) {
            throw new RuntimeException("Failed to replace " + oldJar, e);
        }
    }
}
