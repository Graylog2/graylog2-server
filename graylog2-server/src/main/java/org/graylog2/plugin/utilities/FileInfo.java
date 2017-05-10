/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.utilities;

import com.google.auto.value.AutoValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

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

    @Nullable
    public abstract Object key();

    public abstract long size();

    @Nullable
    public abstract FileTime modificationTime();

    public abstract Path path();

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
        } catch (IOException e) {
            return FileInfo.builder()
                    .key(null)
                    .modificationTime(null)
                    .size(-1L)
                    .path(path)
                    .build();
        }
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
