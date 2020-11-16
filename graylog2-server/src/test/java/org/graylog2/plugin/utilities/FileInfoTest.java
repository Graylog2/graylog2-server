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
package org.graylog2.plugin.utilities;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static org.assertj.core.api.Assertions.assertThat;

public class FileInfoTest {

    @Test
    public void existingFile() throws Exception {
        final File tempFile = File.createTempFile("fileinfo", "test");
        final FileInfo fileInfo = FileInfo.forPath(tempFile.toPath());

        assertThat(fileInfo).isNotNull();
        final FileInfo.Change change = fileInfo.checkForChange();
        assertThat(change).isNotNull();
        assertThat(change.isChanged()).isFalse();

        // this should always succeed
        assertThat(tempFile.delete()).isTrue();

        assertThat(fileInfo.checkForChange().isChanged()).isTrue();
    }

    @Test
    public void missingFile() throws Exception {
        final File tempFile = File.createTempFile("fileinfo", "test");
        final Path path = tempFile.toPath();

        // file is now missing
        assertThat(tempFile.delete()).isTrue();

        final FileInfo fileInfo = FileInfo.forPath(path);

        assertThat(fileInfo).isNotNull();
        final FileInfo.Change change1 = fileInfo.checkForChange();
        assertThat(change1).isNotNull();
        assertThat(change1.isChanged()).isFalse();

        Files.touch(tempFile);

        // sanity check
        assertThat(path.toFile().exists()).isTrue();

        assertThat(fileInfo.checkForChange().isChanged()).isTrue();
    }

    @Test
    public void contentChanged() throws Exception {
        final File tempFile = File.createTempFile("fileinfo", "test");
        final FileInfo fileInfo = FileInfo.forPath(tempFile.toPath());

        // file modification time only has second resolution, so need to sleep a bit longer than a second
        sleepUninterruptibly(1100, TimeUnit.MILLISECONDS);
        FileInfo.Change change;

        Files.touch(tempFile);
        change = fileInfo.checkForChange();
        assertThat(change.isChanged()).isTrue();

        // writing data into the file marks it as changed
        Files.write("test".getBytes(StandardCharsets.US_ASCII), tempFile);
        change = fileInfo.checkForChange();
        assertThat(change.isChanged()).isTrue();

        // file modification time only has second resolution, so need to sleep a bit longer than a second
        sleepUninterruptibly(1100, TimeUnit.MILLISECONDS);
        // replacing the entire content with identical content is considered a change (because modtime changes!).
        Files.write("test".getBytes(StandardCharsets.US_ASCII), tempFile);
        change = fileInfo.checkForChange();
        assertThat(change.isChanged()).isTrue();
    }

    @Test
    public void empty() {
        assertThat(FileInfo.empty()).isEqualTo(FileInfo.builder()
                .path(Paths.get(""))
                .key(null)
                .modificationTime(null)
                .size(-1L)
                .build());
    }
}