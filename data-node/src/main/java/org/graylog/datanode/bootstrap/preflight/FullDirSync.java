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
package org.graylog.datanode.bootstrap.preflight;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FullDirSync {
    private static final Logger LOG = LoggerFactory.getLogger(FullDirSync.class);

    /**
     * The execute bit for directories means that owner can traverse these and access their content.
     */
    protected static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE);
    protected static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ);

    public static void run(Path source, Path target) throws IOException {

        final List<Path> existingPaths = collectExistingPaths(target);
        existingPaths.remove(target); // exclude the target, we don't want to remove it in following step
        deletePaths(existingPaths);
        copyFiles(source, target);
    }

    private static void deletePaths(List<Path> existingPaths) throws IOException {
        for (Path path : existingPaths) {
            if (Files.isDirectory(path)) {
                LOG.info("Deleting obsolete directory " + path);
                FileUtils.deleteDirectory(path.toFile());
            } else {
                try {
                    LOG.info("Deleting obsolete file " + path);
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LOG.info("Failed to delete obsolete file " + path);
                }
            }

        }
    }

    private static List<Path> copyFiles(Path source, Path target) throws IOException {

        List<Path> copiedPaths = new LinkedList<>();

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path currentTarget = target.resolve(source.relativize(dir).toString());
                Files.createDirectories(currentTarget, PosixFilePermissions.asFileAttribute((DIRECTORY_PERMISSIONS)));
                copiedPaths.add(currentTarget);
                LOG.info("Synchronizing directory {}", currentTarget);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final Path currentTarget = target.resolve(source.relativize(file).toString());
                Files.copy(file, currentTarget, StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(currentTarget, FILE_PERMISSIONS);
                copiedPaths.add(currentTarget);
                LOG.info("Synchronizing file {}", currentTarget);
                return FileVisitResult.CONTINUE;
            }
        });

        return copiedPaths;
    }

    private static List<Path> collectExistingPaths(Path target) throws IOException {
        final List<Path> pathsToDelete = new LinkedList<>();

        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                pathsToDelete.add(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                pathsToDelete.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return pathsToDelete;
    }
}
