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
package org.graylog2.indexer.fieldtypes.streamfiltered.storage.model;

import org.graylog2.indexer.fieldtypes.streamfiltered.config.Config;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StoredStreamFieldsTest {

    @Test
    void createsObjectWithCurrentDate() {
        final StoredStreamFields toTest = StoredStreamFields.create("id", Collections.emptySet());
        assertThat(new Duration(toTest.createdAt(), DateTime.now(DateTimeZone.UTC)).getStandardSeconds())
                .isLessThan(2);
    }

    @Test
    void outdatedCheckWorksCorrectly() {
        StoredStreamFields outdated = StoredStreamFields.create("id",
                DateTime.now(DateTimeZone.UTC).minusMinutes(Config.MAX_STORED_FIELDS_AGE_IN_MINUTES + 1),
                Collections.emptySet());
        assertThat(outdated.isOutdated()).isTrue();

        StoredStreamFields fresh = StoredStreamFields.create("id",
                DateTime.now(DateTimeZone.UTC),
                Collections.emptySet());
        assertThat(fresh.isOutdated()).isFalse();
    }

}
