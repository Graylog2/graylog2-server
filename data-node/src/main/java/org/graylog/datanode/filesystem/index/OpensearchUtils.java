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

public class OpensearchUtils {

    // OpenSearch 2.x/3.x version IDs are stored as raw integer ^ MASK; legacy ES IDs are stored as-is.
    // Encoding: major * 1_000_000 + minor * 10_000 + patch * 100 + build
    public static String versionStringFromId(int id) {
        final int raw = id >= 0x08000000 ? id ^ 0x08000000 : id;
        return (raw / 1_000_000) + "." + (raw % 1_000_000 / 10_000) + "." + (raw % 10_000 / 100);
    }

    /**
     * Two OpenSearch versions are compatible if their major versions differ by at most one,
     * mirroring OpenSearch's own Version.isCompatible() contract for versions >= 3.
     */
    public static boolean isCompatible(Version current, Version node) {
        final long diff = current.majorVersion() - node.majorVersion();
        return diff >= -1 && diff <= 1;
    }

}
