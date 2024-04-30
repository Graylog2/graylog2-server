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
package org.graylog2.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.model.Sorts;
import jakarta.ws.rs.BadRequestException;
import org.bson.conversions.Bson;

import java.util.List;

/**
 * Sort order for querying MongoDB to be used as a query param in REST resources.
 */
public enum SortOrder {
    @JsonProperty("asc")
    ASCENDING {
        @Override
        public Bson toBsonSort(String... fields) {
            return Sorts.ascending(fields);
        }

        @Override
        public Bson toBsonSort(List<String> fields) {
            return Sorts.ascending(fields);
        }
    },

    @JsonProperty("desc")
    DESCENDING {
        @Override
        public Bson toBsonSort(String... fields) {
            return Sorts.descending(fields);
        }

        @Override
        public Bson toBsonSort(List<String> fields) {
            return Sorts.descending(fields);
        }
    };

    // Jersey will look for a #fromString method to deserialize a query parameter
    @JsonCreator
    public static SortOrder fromString(String order) {
        return switch (order.toLowerCase()) {
            case "asc" -> ASCENDING;
            case "desc" -> DESCENDING;
            // throwing an IllegalArgumentException here would have Jersey abort with a 404
            default -> throw new BadRequestException("Unknown sort order: " + order);
        };
    }

    /**
     * Generate a Bson sort object that can be used directly as a parameter for querying MongoDB.
     * Calls {@link Sorts#ascending(String...)} or {@link Sorts#descending(String...)} as appropriate.
     *
     * @param fields Fields to sort on in the specified order.
     * @return A BSON object as returned by the {@link Sorts} factory methods
     */
    public abstract Bson toBsonSort(String... fields);

    /**
     * Generate a Bson sort object that can be used directly as a parameter for querying MongoDB.
     * Calls {@link Sorts#ascending(List)} or {@link Sorts#descending(List)} as appropriate.
     *
     * @param fields Fields to sort on in the specified order.
     * @return A BSON object as returned by the {@link Sorts} factory methods
     */
    public abstract Bson toBsonSort(List<String> fields);
}
