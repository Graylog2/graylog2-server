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
package org.graylog2.storage.versionprobe;

import com.google.auto.value.AutoValue;
import org.graylog2.plugin.Version;

import javax.annotation.Nullable;

@AutoValue
public abstract class SearchVersion {

    public SearchVersion major() {
        return create(distribution(), Version.from(version().getVersion().getMajorVersion(), 0, 0));
    }

    @Nullable
    public abstract String distribution();

    public abstract Version version();

    public static SearchVersion withoutDistribution(final Version version) {
        return create(null, version);
    }

    public static SearchVersion create(@Nullable final String distribution, final Version version) {
        return new AutoValue_SearchVersion(distribution, version);
    }

    @Override
    public String toString() {
        if (distribution() != null) {
            return distribution() + ":" + version();
        } else {
            return version().toString();
        }
    }
}
