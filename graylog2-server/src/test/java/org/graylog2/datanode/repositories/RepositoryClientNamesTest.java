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
package org.graylog2.datanode.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryClientNamesTest {

    @Test
    void generatedNameContainsTheRepositoryNameAndASuffix() {
        assertThat(RepositoryClientNames.generate("archive-eu")).matches("repository_archive-eu_[0-9a-f]{4}");
    }

    @ParameterizedTest
    @CsvSource({
            "archive-eu, archive-eu",
            "Logs.2026:cold, logs-2026-cold",
            "a..b, a-b",
            "-._~edge~._-, edge",
            "!$&()+=:@, unnamed",
            "abcdefghijklmnopqrstuvwxyz0123456789, abcdefghijklmnopqrstuvwxyz012345",
            "abcdefghijklmnopqrstuvwxyz01234.6789, abcdefghijklmnopqrstuvwxyz01234",
    })
    void slugOnlyContainsCharactersAllowedInSettingKeys(String repositoryName, String slug) {
        assertThat(RepositoryClientNames.slug(repositoryName)).isEqualTo(slug);
    }

    @Test
    void generatedNamesDifferForTheSameRepositoryName() {
        final Set<String> names = new HashSet<>();
        IntStream.range(0, 20).forEach(i -> names.add(RepositoryClientNames.generate("repo")));

        // A repository re-created under the same name must get a new client
        assertThat(names).hasSizeGreaterThan(1);
    }

    @Test
    void generatedNamesAreManaged() {
        assertThat(RepositoryClientNames.isManaged(RepositoryClientNames.generate("Logs.2026:cold"))).isTrue();
        assertThat(RepositoryClientNames.isManaged(RepositoryClientNames.generate("!$&"))).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"default", "custom", "repository_", "repository_custom", "repository_repo_7f3", "repository_repo_7F3A",
            "x_repo_7f3a", "repository_repo.x_7f3a"})
    void clientsNotGeneratedByGraylogAreNotManaged(String client) {
        assertThat(RepositoryClientNames.isManaged(client)).isFalse();
    }
}
