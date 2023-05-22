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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.apache.commons.lang3.function.TriFunction;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.FieldDecorator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.cert.ocsp.Req;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TabularResponseCreatorTest {

    @Test
    void decorate() {
        final TabularResponseCreator creator = new TabularResponseCreator() {};
        final Set<FieldDecorator> decorators = Collections.singleton(
                decorator(
                        (field) -> Objects.equals(field.decorator(), "uppercase"),
                        (field, o, searchUser) -> String.valueOf(o).toUpperCase(Locale.ROOT))
        );
        final Object decorated = creator.decorate(decorators, RequestedField.parse("myfield.uppercase"), "my-value", TestSearchUser.builder().build());
        Assertions.assertThat(decorated).isEqualTo("MY-VALUE");
    }

    @Test
    void noDecoratorFound() {
        final TabularResponseCreator creator = new TabularResponseCreator() {};
        final Set<FieldDecorator> decorators = Collections.singleton(
                decorator(
                        (field) -> Objects.equals(field.decorator(), "uppercase"),
                        (field, o, searchUser) -> String.valueOf(o).toUpperCase(Locale.ROOT))
        );

        Assertions.assertThatThrownBy(() -> creator.decorate(decorators, RequestedField.parse("myfield.lowercase"), "my-value", TestSearchUser.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported property 'lowercase' on field 'myfield'");
    }


    @Test
    void moreDecoratorFound() {
        final TabularResponseCreator creator = new TabularResponseCreator() {};
        final Set<FieldDecorator> decorators = Set.of(
                decorator(
                        (field) -> true,
                        (field, o, searchUser) -> String.valueOf(o).toUpperCase(Locale.ROOT)),
                decorator(
                        (field) -> true,
                        (field, o, searchUser) -> String.valueOf(o).toLowerCase(Locale.ROOT))
        );

        Assertions.assertThatThrownBy(() -> creator.decorate(decorators, RequestedField.parse("myfield.lowercase"), "my-value", TestSearchUser.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Found more decorators supporting 'lowercase' on field 'myfield', this is not supported operation.");
    }

    private static FieldDecorator decorator(Predicate<RequestedField> accept, TriFunction<RequestedField, Object, SearchUser, Object> decorate) {
        return new FieldDecorator() {
            @Override
            public boolean accept(RequestedField field) {
                return accept.test(field);
            }

            @Override
            public Object decorate(RequestedField field, Object value, SearchUser searchUser) {
                return decorate.apply(field, value, searchUser);
            }
        };
    }
}
