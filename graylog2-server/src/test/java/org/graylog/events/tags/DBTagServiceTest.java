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
package org.graylog.events.tags;

import com.mongodb.MongoWriteException;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class DBTagServiceTest {

    private DBTagService dbTagService;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        dbTagService = new DBTagService(mongoCollections);
    }

    @Test
    void saveAssignsId() {
        final Tag saved = dbTagService.save(Tag.builder().value("phishing").build());
        assertThat(saved.id()).isNotNull();
        assertThat(saved.value()).isEqualTo("phishing");
    }

    @Test
    void getByIdRoundTrips() {
        final Tag saved = dbTagService.save(Tag.builder().value("malware").build());
        assertThat(dbTagService.get(saved.id())).contains(saved);
    }

    @Test
    void getByValueFinds() {
        dbTagService.save(Tag.builder().value("ransomware").build());
        assertThat(dbTagService.getByValue("ransomware")).isPresent();
        assertThat(dbTagService.getByValue("missing")).isEmpty();
    }

    @Test
    void uniqueIndexBlocksDuplicateValue() {
        dbTagService.save(Tag.builder().value("dup").build());
        assertThatThrownBy(() -> dbTagService.save(Tag.builder().value("dup").build()))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void updateRenamesValue() {
        final Tag saved = dbTagService.save(Tag.builder().value("old").build());
        dbTagService.update(saved.id(), "new");
        assertThat(dbTagService.get(saved.id()).orElseThrow().value()).isEqualTo("new");
    }

    @Test
    void deleteRemovesEntity() {
        final Tag saved = dbTagService.save(Tag.builder().value("temp").build());
        assertThat(dbTagService.delete(saved.id())).isEqualTo(1L);
        assertThat(dbTagService.get(saved.id())).isEmpty();
    }

    @Test
    void findPaginatedReturnsResults() {
        dbTagService.save(Tag.builder().value("a-tag").build());
        dbTagService.save(Tag.builder().value("b-tag").build());

        final PaginatedList<Tag> page = dbTagService.findPaginated("", 1, 10, SortOrder.ASCENDING, "value", null);
        assertThat(page.delegate()).hasSize(2);
        assertThat(page.delegate().get(0).value()).isEqualTo("a-tag");
    }

    @Test
    void streamAllReturnsEverything() {
        dbTagService.save(Tag.builder().value("x").build());
        dbTagService.save(Tag.builder().value("y").build());

        try (Stream<Tag> stream = dbTagService.streamAll()) {
            final List<Tag> all = stream.toList();
            assertThat(all).hasSize(2);
        }
    }
}
