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

import com.google.auto.value.AutoValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * A {@code FileInfo} presents a concise way of checking for file modification based on its file system attributes.
 * <p>
 *     Construct it via its {@link FileInfo#forPath(Path)} method and later use the {@link #checkForChange()} method
 *     whenever you want to act if a modification has occurred. The returned {@link Change} object contains whether
 *     the file has actually changed and the new file info object to use in future checks.
 * </p>
 */
@AutoValue
public abstract class FileInfo {

    private static final Logger LOG = LoggerFactory.getLogger(FileInfo.class);
    private static final FileInfo EMPTY_FILE_INFO = FileInfo.builder()
            .key(null)
            .modificationTime(null)
            .size(-1L)
            .path(Paths.get(""))
            .build();

    @Nullable
    public abstract Object key();

    public abstract long size();

    @Nullable
    public abstract FileTime modificationTime();

    public abstract Path path();

    protected abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_FileInfo.Builder();
    }

    /**
     * Create a file info for the given path.
     *
     * @param path the path must exist, otherwise an IllegalArgumentException is thrown
     * @return the file info object
     */
    @NotNull
    public static FileInfo forPath(Path path) {
        try {
            final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return FileInfo.builder()
                    .path(path)
                    .key(attributes.fileKey())
                    .size(attributes.size())
                    .modificationTime(attributes.lastModifiedTime())
                    .build();
        } catch (Exception e) {
            LOG.error("Couldn't get file info for path: {}", path, e);
            return EMPTY_FILE_INFO.toBuilder().path(path).build();
        }
    }

    @NotNull
    public static FileInfo empty() {
        return EMPTY_FILE_INFO;
    }

    @NotNull
    public FileInfo.Change checkForChange() {
        final FileInfo newFileInfo = forPath(path());
        if (newFileInfo.equals(this)) {
            return Change.none();
        }
        return new Change(newFileInfo);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder key(@Nullable Object key);

        public abstract Builder size(long size);

        public abstract Builder modificationTime(@Nullable FileTime modificationTime);

        public abstract Builder path(Path path);

        public abstract FileInfo build();
    }

    public static class Change {
        private static final Change NONE = new Change(null);
        private final FileInfo info;

        public Change(FileInfo info) {
            this.info = info;
        }

        public static Change none() {
            return NONE;
        }

        public boolean isChanged() {
            return info != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Change change = (Change) o;
            return info.equals(change.info);
        }

        @Override
        public int hashCode() {
            return Objects.hash(info);
        }

        @Nullable
        public FileInfo fileInfo() {
            return info;
        }
    }
}
