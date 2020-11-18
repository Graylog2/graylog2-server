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
package org.graylog2.decorators;

import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class DecoratorServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private DecoratorServiceImpl decoratorService;

    @Before
    public void setUp() {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        final MongoJackObjectMapperProvider provider = new MongoJackObjectMapperProvider(objectMapperProvider.get());
        decoratorService = new DecoratorServiceImpl(mongodb.mongoConnection(), provider);
    }

    @Test
    @MongoDBFixtures("DecoratorServiceImplTest.json")
    public void findForStreamReturnsDecoratorsForStream() {
        assertThat(decoratorService.findForStream("000000000000000000000001"))
                .hasSize(2)
                .extracting(Decorator::id)
                .containsExactly("588bcafebabedeadbeef0001", "588bcafebabedeadbeef0002");
        assertThat(decoratorService.findForStream("000000000000000000000002")).isEmpty();
    }

    @Test
    @MongoDBFixtures("DecoratorServiceImplTest.json")
    public void findForGlobalReturnsDecoratorForGlobalStream() {
        assertThat(decoratorService.findForGlobal())
                .hasSize(1)
                .extracting(Decorator::id)
                .containsOnly("588bcafebabedeadbeef0003");
    }

    @Test
    @MongoDBFixtures("DecoratorServiceImplTest.json")
    public void findByIdReturnsValidDecorator() throws NotFoundException {
        final Decorator decorator = decoratorService.findById("588bcafebabedeadbeef0001");
        assertThat(decorator.id()).isEqualTo("588bcafebabedeadbeef0001");
        assertThat(decorator.order()).isEqualTo(0);
        assertThat(decorator.stream())
                .isPresent()
                .contains("000000000000000000000001");
    }

    @Test
    public void findByIdThrowsNotFoundExceptionForMissingDecorator() throws NotFoundException {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Decorator with id 588bcafebabedeadbeef0001 not found.");

        decoratorService.findById("588bcafebabedeadbeef0001");
    }

    @Test
    public void findByIdThrowsIllegalArgumentExceptionForInvalidObjectId() throws NotFoundException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid hexadecimal representation of an ObjectId: [NOPE]");

        decoratorService.findById("NOPE");
    }

    @Test
    @MongoDBFixtures("DecoratorServiceImplTest.json")
    public void findAllReturnsAllDecorators() {
        assertThat(decoratorService.findAll())
                .hasSize(3)
                .extracting(Decorator::id)
                .containsExactly("588bcafebabedeadbeef0001", "588bcafebabedeadbeef0002", "588bcafebabedeadbeef0003");
    }

    @Test
    public void createWithoutStreamCreatesGlobalDecorator() {
        final Decorator decorator = decoratorService.create("type", singletonMap("foo", "bar"), 42);
        assertThat(decorator.id()).isNull();
        assertThat(decorator.type()).isEqualTo("type");
        assertThat(decorator.order()).isEqualTo(42);
        assertThat(decorator.config())
                .hasSize(1)
                .containsEntry("foo", "bar");
        assertThat(decorator.stream()).isEmpty();
    }

    @Test
    public void createWithStreamCreatesDecorator() {
        final Decorator decorator = decoratorService.create("type", singletonMap("foo", "bar"), "000000000000000000000001", 42);
        assertThat(decorator.id()).isNull();
        assertThat(decorator.type()).isEqualTo("type");
        assertThat(decorator.order()).isEqualTo(42);
        assertThat(decorator.config())
                .hasSize(1)
                .containsEntry("foo", "bar");
        assertThat(decorator.stream())
                .isPresent()
                .contains("000000000000000000000001");
    }

    @Test
    public void saveWritesDecoratorToDatabase() throws NotFoundException {
        final Decorator decorator = decoratorService.create("type", singletonMap("foo", "bar"), "000000000000000000000001", 42);

        final Decorator savedDecorator = decoratorService.save(decorator);
        assertThat(savedDecorator).isEqualToIgnoringNullFields(decorator);
        assertThat(savedDecorator.stream())
                .isPresent()
                .contains("000000000000000000000001");

        final Decorator loadedDecorator = decoratorService.findById(savedDecorator.id());
        assertThat(loadedDecorator).isEqualTo(savedDecorator);
    }

    @Test
    public void saveWritesGlobalDecoratorToDatabase() throws NotFoundException {
        final Decorator decorator = decoratorService.create("type", singletonMap("foo", "bar"), 42);

        final Decorator savedDecorator = decoratorService.save(decorator);
        assertThat(savedDecorator).isEqualToIgnoringNullFields(decorator);
        assertThat(savedDecorator.stream()).isEmpty();

        final Decorator loadedDecorator = decoratorService.findById(savedDecorator.id());
        assertThat(loadedDecorator).isEqualTo(savedDecorator);
    }

    @Test
    @MongoDBFixtures("DecoratorServiceImplTest.json")
    public void delete() {
        assertThat(decoratorService.findAll()).hasSize(3);
        assertThat(decoratorService.delete("588bcafebabedeadbeef0001")).isEqualTo(1);
        assertThat(decoratorService.findAll()).hasSize(2);
        assertThat(decoratorService.delete("588bcafebabedeadbeef0001")).isEqualTo(0);
        assertThat(decoratorService.delete("588bcafebabedeadbeef9999")).isEqualTo(0);
    }

    @Test
    public void deleteThrowsIllegalArgumentExceptionForInvalidObjectId() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid hexadecimal representation of an ObjectId: [NOPE]");

        decoratorService.delete("NOPE");
    }
}
