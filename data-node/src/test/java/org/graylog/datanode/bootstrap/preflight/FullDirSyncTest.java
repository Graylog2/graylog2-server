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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FullDirSyncTest {

    @TempDir
    private Path source;

    @TempDir
    private Path target;

    @BeforeEach
    void setUp() throws IOException {

        /*
        source:
        -a.txt
        -b.txt
        -subdir
          -c.txt

        target:
        -a.txt
        -x.txt
        -empty-dir
        -subdir
          -y.txt
        -unused-dir
          -z.txt
         */

        Files.createFile(source.resolve("a.txt"));
        Files.createFile(source.resolve("b.txt"));
        Files.createDirectories(source.resolve("subdir"));
        Files.createFile(source.resolve("subdir").resolve("c.txt"));

        Files.createFile(target.resolve("a.txt"));
        Files.createFile(target.resolve("x.txt"));
        Files.createDirectories(target.resolve("subdir"));
        Files.createDirectories(target.resolve("empty-dir"));
        Files.createFile(target.resolve("subdir").resolve("y.txt"));
        Files.createDirectories(target.resolve("unused-dir"));
        Files.createFile(target.resolve("unused-dir").resolve("z.txt"));
    }

    @Test
    void run() throws IOException {
        FullDirSync.run(source, target);

        List<Path> afterSyncState = new ArrayList<>();

        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(!target.equals(dir)) {
                    afterSyncState.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                afterSyncState.add(file);
                return super.visitFile(file, attrs);
            }
        });

        Assertions.assertThat(afterSyncState)
                .extracting(target::relativize)
                .extracting(Path::toString)
                .hasSize(4)
                .contains("a.txt", "b.txt", "subdir", "subdir/c.txt");

    }
}
