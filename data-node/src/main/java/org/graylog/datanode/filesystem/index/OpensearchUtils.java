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

    /**
     * The last elasticsearch major version, which wrote the same Lucene 8 index format as opensearch 1.x.
     */
    private static final long LEGACY_ES_MAJOR_SHARING_OPENSEARCH1_FORMAT = 7;

    /**
     * The opensearch major version that shares its index format with elasticsearch 7.x.
     */
    private static final long OPENSEARCH_MAJOR_SHARING_ES7_FORMAT = 1;

    // OpenSearch 2.x/3.x version IDs are stored as raw integer ^ MASK; legacy ES IDs are stored as-is.
    // Encoding: major * 1_000_000 + minor * 10_000 + patch * 100 + build
    public static String versionStringFromId(int id) {
        final int raw = id >= 0x08000000 ? id ^ 0x08000000 : id;
        return (raw / 1_000_000) + "." + (raw % 1_000_000 / 10_000) + "." + (raw % 10_000 / 100);
    }

    /**
     * The index generation a version belongs to.
     * <p>
     * Version numbers alone don't tell generations apart: an index written by elasticsearch 7.x has the same format
     * as one written by opensearch 1.x, because opensearch 1.0 forked from elasticsearch 7.10 and every 7.x release
     * wrote the Lucene 8 format that opensearch 1.x kept writing. Comparing the raw majors puts the two six
     * generations apart, which is why comparisons of node and index versions have to run on the generation instead.
     * <p>
     * Elasticsearch 6.x and older wrote Lucene 7, which has no opensearch counterpart. Those keep their own major
     * version, stay incompatible with everything shipped here, and are already rejected by the index readers.
     */
    public static long generation(Version version) {
        return version.majorVersion() == LEGACY_ES_MAJOR_SHARING_OPENSEARCH1_FORMAT
                ? OPENSEARCH_MAJOR_SHARING_ES7_FORMAT
                : version.majorVersion();
    }

    /**
     * Two versions are compatible if their generations differ by at most one, mirroring OpenSearch's own
     * Version.isCompatible() contract for versions >= 3.
     */
    public static boolean isCompatible(Version current, Version node) {
        final long diff = generation(current) - generation(node);
        return diff >= -1 && diff <= 1;
    }

}
