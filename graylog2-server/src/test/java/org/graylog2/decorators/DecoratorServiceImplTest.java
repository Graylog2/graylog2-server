/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.decorators;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class DecoratorServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("decorators_test");

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private DecoratorServiceImpl decoratorService;

    @Before
    public void setUp() {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        final MongoJackObjectMapperProvider provider = new MongoJackObjectMapperProvider(objectMapperProvider.get());
        decoratorService = new DecoratorServiceImpl(mongoRule.getMongoConnection(), provider);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findForStreamReturnsDecoratorsForStream() {
        assertThat(decoratorService.findForStream("000000000000000000000001"))
                .hasSize(2)
                .extracting(Decorator::id)
                .containsExactly("588bcafebabedeadbeef0001", "588bcafebabedeadbeef0002");
        assertThat(decoratorService.findForStream("000000000000000000000002")).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findForGlobalReturnsDecoratorForGlobalStream() {
        assertThat(decoratorService.findForGlobal())
                .hasSize(1)
                .extracting(Decorator::id)
                .containsOnly("588bcafebabedeadbeef0003");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByIdReturnsValidDecorator() throws NotFoundException {
        final Decorator decorator = decoratorService.findById("588bcafebabedeadbeef0001");
        assertThat(decorator.id()).isEqualTo("588bcafebabedeadbeef0001");
        assertThat(decorator.order()).isEqualTo(0);
        assertThat(decorator.stream())
                .isPresent()
                .contains("000000000000000000000001");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void findByIdThrowsNotFoundExceptionForMissingDecorator() throws NotFoundException {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Decorator with id 588bcafebabedeadbeef0001 not found.");

        decoratorService.findById("588bcafebabedeadbeef0001");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void findByIdThrowsIllegalArgumentExceptionForInvalidObjectId() throws NotFoundException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid hexadecimal representation of an ObjectId: [NOPE]");

        decoratorService.findById("NOPE");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveWritesGlobalDecoratorToDatabase() throws NotFoundException {
        final Decorator decorator = decoratorService.create("type", singletonMap("foo", "bar"), 42);

        final Decorator savedDecorator = decoratorService.save(decorator);
        assertThat(savedDecorator).isEqualToIgnoringNullFields(decorator);
        assertThat(savedDecorator.stream()).isEmpty();

        final Decorator loadedDecorator = decoratorService.findById(savedDecorator.id());
        assertThat(loadedDecorator).isEqualTo(savedDecorator);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() {
        assertThat(decoratorService.findAll()).hasSize(3);
        assertThat(decoratorService.delete("588bcafebabedeadbeef0001")).isEqualTo(1);
        assertThat(decoratorService.findAll()).hasSize(2);
        assertThat(decoratorService.delete("588bcafebabedeadbeef0001")).isEqualTo(0);
        assertThat(decoratorService.delete("588bcafebabedeadbeef9999")).isEqualTo(0);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void deleteThrowsIllegalArgumentExceptionForInvalidObjectId() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid hexadecimal representation of an ObjectId: [NOPE]");

        decoratorService.delete("NOPE");
    }
}