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
package org.mongojack;

import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WriteResultTest {

    @Test
    void toIdType() {
        assertThat(WriteResult.toIdType(new BsonString("string-id"), String.class)).isEqualTo("string-id");
        assertThat(WriteResult.toIdType(new BsonObjectId(new ObjectId("54e3deadbeefdeadbeef0001")), String.class)).
                isEqualTo("54e3deadbeefdeadbeef0001");
        assertThat(WriteResult.toIdType(new BsonObjectId(new ObjectId("54e3deadbeefdeadbeef0001")), ObjectId.class)).
                isEqualTo(new ObjectId("54e3deadbeefdeadbeef0001"));
    }
}
