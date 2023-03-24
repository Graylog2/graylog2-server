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
package org.graylog2.database.dbcatalog;

import org.graylog2.streams.StreamImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbEntitiesCatalogTest {

    @Test
    void returnsEmptyOptionalsOnEmptyCatalog() {
        DbEntitiesCatalog catalog = new DbEntitiesCatalog(List.of());

        assertThat(catalog.getByCollectionName("Guadalajara")).isEmpty();
        assertThat(catalog.getByModelClass(Object.class)).isEmpty();
    }

    @Test
    void returnsEmptyOptionalsOnEntryAbsentInCatalog() {
        DbEntitiesCatalog catalog = new DbEntitiesCatalog(List.of(new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read")));

        assertThat(catalog.getByCollectionName("Guadalajara")).isEmpty();
        assertThat(catalog.getByModelClass(Object.class)).isEmpty();
    }

    @Test
    void returnsProperDataFromCatalog() {
        DbEntitiesCatalog catalog = new DbEntitiesCatalog(List.of(new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read")));

        assertThat(catalog.getByCollectionName("streams"))
                .isEqualTo(Optional.of(
                        new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read")
                        )
                );
        assertThat(catalog.getByModelClass(StreamImpl.class))
                .isEqualTo(Optional.of(
                        new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read")
                        )
                );
    }

    @Test
    void throwsExceptionOnTwoEntriesPointingToTheSameCollection() {
        assertThrows(IllegalStateException.class,
                () -> new DbEntitiesCatalog(
                        List.of(
                                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read"),
                                new DbEntityCatalogEntry("streams", "title", String.class, "")
                        )
                )
        );
    }
}
