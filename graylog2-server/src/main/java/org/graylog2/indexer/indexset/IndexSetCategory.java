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
package org.graylog2.indexer.indexset;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.graylog2.rest.resources.entities.FilterOption;

import java.util.Arrays;
import java.util.Locale;

import static org.graylog2.indexer.indexset.IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_REGULAR;
import static org.graylog2.indexer.indexset.fields.IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE;
import static org.graylog2.indexer.indexset.fields.WritableField.FIELD_WRITABLE;
import static org.graylog2.indexer.template.IndexTemplateProvider.ILLUMINATE_INDEX_TEMPLATE_TYPE;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Groups index sets for the entity table's category filter and counts.
 * <p>
 * The MongoDB predicates mirror {@link IndexSetConfig#isRegularIndex()}. A non-writable index set is never
 * regular. An explicit {@code regular} flag wins. Otherwise the default template type decides. The Illuminate
 * template type takes precedence over all of that. {@link #of(IndexSetConfig)} classifies in Java; a test
 * keeps both in agreement.
 */
public enum IndexSetCategory {
    USER("user", "User"),
    SYSTEM("system", "System"),
    ILLUMINATE("illuminate", "Illuminate");

    private static final Bson ILLUMINATE_TEMPLATE = Filters.eq(FIELD_INDEX_TEMPLATE_TYPE, ILLUMINATE_INDEX_TEMPLATE_TYPE);
    private static final Bson NOT_ILLUMINATE_TEMPLATE = Filters.ne(FIELD_INDEX_TEMPLATE_TYPE, ILLUMINATE_INDEX_TEMPLATE_TYPE);
    // In MongoDB, {field: null} matches a null value and a missing field. The Java fallbacks treat both as unset.
    private static final Bson REGULAR = Filters.and(
            Filters.ne(FIELD_WRITABLE, false),
            Filters.or(
                    Filters.eq(FIELD_REGULAR, true),
                    Filters.and(
                            Filters.eq(FIELD_REGULAR, null),
                            Filters.or(
                                    Filters.eq(FIELD_INDEX_TEMPLATE_TYPE, null),
                                    Filters.eq(FIELD_INDEX_TEMPLATE_TYPE, DEFAULT_INDEX_TEMPLATE_TYPE)))));

    private final String value;
    private final String title;

    IndexSetCategory(String value, String title) {
        this.value = value;
        this.title = title;
    }

    /** The wire value used in filters, counts, and the element payload. */
    public String value() {
        return value;
    }

    public FilterOption filterOption() {
        return FilterOption.create(value, title);
    }

    /** Selects exactly the index sets of this category. */
    public Bson toBson() {
        return switch (this) {
            case ILLUMINATE -> ILLUMINATE_TEMPLATE;
            case USER -> Filters.and(NOT_ILLUMINATE_TEMPLATE, REGULAR);
            case SYSTEM -> Filters.and(NOT_ILLUMINATE_TEMPLATE, Filters.nor(REGULAR));
        };
    }

    public static IndexSetCategory of(IndexSetConfig config) {
        if (config.indexTemplateType().map(ILLUMINATE_INDEX_TEMPLATE_TYPE::equals).orElse(false)) {
            return ILLUMINATE;
        }
        return config.isRegularIndex() ? USER : SYSTEM;
    }

    /** @throws IllegalArgumentException for a value that names no category. The filter parser maps it to a 400 response. */
    public static IndexSetCategory fromValue(String value) {
        final String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(f("Unknown index set category: %s", value)));
    }
}
