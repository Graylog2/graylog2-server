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
package org.graylog.datanode.filesystem.index;

import com.github.zafarkhaja.semver.Version;

/**
 * Which of the two opensearch distributions shipped with the datanode is able to open an index directory. An index
 * format is only readable by the opensearch release that wrote it and the one following it, so data written by an
 * older indexer needs the compatibility distribution before the current one can take over.
 */
public enum RequiredOpensearchDistribution {

    /**
     * The current opensearch distribution can open the directory directly.
     */
    CURRENT(3),

    /**
     * The directory predates the current distribution and has to be opened by the bundled compatibility
     * distribution ({@code opensearch.compat.version}) first. This is not an error, only a migration step.
     */
    COMPAT(2);

    private final long majorVersion;

    RequiredOpensearchDistribution(long majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * The opensearch major version this tier stands for. These two constants are the single place to edit when a new
     * generation ships and the tiers move up.
     */
    public long majorVersion() {
        return majorVersion;
    }

    /**
     * Whether the given opensearch version belongs to this tier. Only the major version is compared.
     */
    public boolean matches(String opensearchVersion) {
        return Version.parse(opensearchVersion).majorVersion() == majorVersion;
    }
}
