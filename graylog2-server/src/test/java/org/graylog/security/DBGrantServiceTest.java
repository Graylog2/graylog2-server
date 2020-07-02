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
package org.graylog.security;

import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.utilities.GRN;
import org.graylog2.utilities.GRNRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DBGrantServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private DBGrantService dbService;
    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapper = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        this.dbService = new DBGrantService(mongodb.mongoConnection(), mapper, grnRegistry);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void test() {
        assertThat(dbService.streamAll().collect(Collectors.toSet()).size()).isEqualTo(4);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForGranteesOrGlobal() {
        final GRN jane = grnRegistry.newGRN("user", "jane");
        final GRN john = grnRegistry.newGRN("user", "john");

        assertThat(dbService.getForGranteesOrGlobal(Collections.singleton(jane))).hasSize(3);
        assertThat(dbService.getForGranteesOrGlobal(Collections.singleton(john))).hasSize(2);
    }
}