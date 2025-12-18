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
package org.graylog2.events;

import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.bson.Document;
import org.graylog2.database.MongoConnection;

import java.util.function.Supplier;

public class OffsetFromCurrentMongoDBTimeProvider implements Provider<Offset> {
    private final Supplier<Offset> mongoConnectionSupplier;

    @Inject
    public OffsetFromCurrentMongoDBTimeProvider(MongoConnection mongoConnection) {
        this.mongoConnectionSupplier = Suppliers.memoize(() -> {
            final var localTime = mongoConnection.getMongoDatabase().runCommand(new Document("hello", 1)).getDate("localTime");
            return new Offset(localTime, null);
        });
    }

    @Override
    public Offset get() {
        return mongoConnectionSupplier.get();
    }
}
