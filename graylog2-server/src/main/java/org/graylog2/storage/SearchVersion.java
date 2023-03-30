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
package org.graylog2.storage;

import com.github.zafarkhaja.semver.Version;
import com.google.auto.value.AutoValue;
import org.graylog2.configuration.validators.SearchVersionRange;
import org.graylog2.indexer.ElasticsearchException;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

@AutoValue
public abstract class SearchVersion {

    public enum Distribution {
        ELASTICSEARCH("Elasticsearch"),
        OPENSEARCH("OpenSearch");

        private final String printName;

        Distribution(String printName) {
            this.printName = printName;
        }

        @Override
        public String toString() {
            return this.printName;
        }
    }

    public abstract Distribution distribution();
    public abstract Version version();

    public SearchVersion major() {
        return create(distribution(), Version.forIntegers(version().getMajorVersion(), 0, 0));
    }

    public boolean satisfies(final Distribution distribution, final String expression) {
        return this.distribution().equals(distribution) && version().satisfies(expression);
    }

    public boolean satisfies(SearchVersionRange range) {
        return satisfies(range.distribution(), range.expression());
    }

    public boolean satisfies(Collection<SearchVersionRange> ranges) {
        for (SearchVersionRange range : ranges) {
            if (satisfies(range)) {
                return true;
            }
        }
        return false;
    }

    public static SearchVersion elasticsearch(final String version) {
        return elasticsearch(parseVersion(version));
    }

    public static SearchVersion elasticsearch(final Version version) {
        return create(Distribution.ELASTICSEARCH, version);
    }

    public static SearchVersion elasticsearch(final int major, final int minor, final int patch) {
        return create(Distribution.ELASTICSEARCH, Version.forIntegers(major, minor, patch));
    }

    public static SearchVersion opensearch(final String version) {
        return opensearch(parseVersion(version));
    }

    public static SearchVersion opensearch(final Version version) {
        return create(Distribution.OPENSEARCH, version);
    }

    public static SearchVersion opensearch(final int major, final int minor, final int patch) {
        return create(Distribution.OPENSEARCH, Version.forIntegers(major, minor, patch));
    }

    public String encode() {
        return String.format(Locale.ROOT, "%s:%s", this.distribution().name().toUpperCase(Locale.ROOT), this.version());
    }

    public static SearchVersion decode(final String searchServerIdentifier) {
        final String[] parts = searchServerIdentifier.split(":");
        if (parts.length == 2) {
            return SearchVersion.create(Distribution.valueOf(parts[0].toUpperCase(Locale.ROOT)), Version.valueOf((parts[1])));
        } else {
            return SearchVersion.elasticsearch(searchServerIdentifier);
        }
    }

    /**
     * @param distribution Assumes ELASTICSEARCH by default when no distribution is provided
     */
    public static SearchVersion create(@Nullable final String distribution, final Version version) {
        final Distribution dst = Optional.ofNullable(distribution)
                .map(String::trim)
                .map(d -> d.toUpperCase(Locale.ROOT))
                .map(Distribution::valueOf)
                .orElse(Distribution.ELASTICSEARCH);
        return new AutoValue_SearchVersion(dst, version);
    }

    public static SearchVersion create(final Distribution distribution, final Version version) {
        return new AutoValue_SearchVersion(distribution, version);
    }

    protected static com.github.zafarkhaja.semver.Version parseVersion(final String version) {
        try {
            return Version.valueOf(version);
        } catch (Exception e) {
            throw new ElasticsearchException("Unable to parse Elasticsearch version: " + version, e);
        }
    }

    @Override
    public String toString() {
        return distribution() + ":" + version();
    }

    public boolean isElasticsearch() {
        return this.distribution().equals(Distribution.ELASTICSEARCH);
    }

    public boolean isOpenSearch() {
        return this.distribution().equals(Distribution.OPENSEARCH);
    }
}
