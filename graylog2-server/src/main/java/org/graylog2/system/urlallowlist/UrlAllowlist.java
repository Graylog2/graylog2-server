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
package org.graylog2.system.urlallowlist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonAutoDetect
@AutoValue
public abstract class UrlAllowlist {

    @JsonProperty("entries")
    public abstract List<AllowlistEntry> entries();

    @JsonProperty("disabled")
    public abstract boolean disabled();

    @JsonCreator
    public static UrlAllowlist create(@JsonProperty("entries") List<AllowlistEntry> entries,
                                      @JsonProperty("disabled") boolean disabled) {
        return builder().entries(entries)
                .disabled(disabled)
                .build();
    }

    public static UrlAllowlist createEnabled(List<AllowlistEntry> entries) {
        return builder().entries(entries)
                .disabled(false)
                .build();
    }

    public abstract Builder toBuilder();

    /**
     * Checks if a URL is allowlisted by looking for an allowlist entry matching the given url.
     *
     * @param url The URL to check.
     * @return {@code false} if the allowlist is enabled and no allowlist entry matches the given url. {@code true} if
     *         there is an allowlist entry matching the given url or if the allowlist is disabled.
     */
    public boolean isAllowlisted(String url) {
        if (disabled()) {
            return true;
        }
        return entries().stream()
                .anyMatch(e -> e.isAllowlisted(url));
    }

    public static Builder builder() {
        return new AutoValue_UrlAllowlist.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder entries(List<AllowlistEntry> entries);

        public abstract Builder disabled(boolean disabled);

        public abstract UrlAllowlist autoBuild();

        public UrlAllowlist build() {
            final UrlAllowlist allowlist = autoBuild();
            checkForDuplicateIds(allowlist.entries());
            return allowlist;
        }

        private void checkForDuplicateIds(List<AllowlistEntry> entries) {
            Set<String> ids = new HashSet<>();
            entries.forEach(entry -> {
                if (ids.contains(entry.id())) {
                    throw new IllegalArgumentException(
                            "Found duplicate ID '" + entry.id() + "' in allowlist entries.");
                }
                ids.add(entry.id());
            });
        }
    }
}
