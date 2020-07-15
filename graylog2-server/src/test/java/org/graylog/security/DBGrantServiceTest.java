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

import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
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
        assertThat(dbService.streamAll().collect(Collectors.toSet()).size()).isEqualTo(6);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForGranteesOrGlobal() {
        final GRN jane = grnRegistry.newGRN("user", "jane");
        final GRN john = grnRegistry.newGRN("user", "john");

        assertThat(dbService.getForGranteesOrGlobal(Collections.singleton(jane))).hasSize(4);
        assertThat(dbService.getForGranteesOrGlobal(Collections.singleton(john))).hasSize(3);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForTarget() {
        final GRN stream1 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0000");
        final GRN stream2 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0001");

        assertThat(dbService.getForTarget(stream1)).hasSize(1);
        assertThat(dbService.getForTarget(stream2)).hasSize(3);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForTargetExcludingGrantee() {
        final GRN stream = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0001");
        final GRN grantee = grnRegistry.parse("grn::::user:john");

        assertThat(dbService.getForTargetExcludingGrantee(stream, grantee)).hasSize(2);
    }
    @Test
    @MongoDBFixtures("grants.json")
    public void getOwnersForTargets() {
        final GRN jane = grnRegistry.parse("grn::::user:jane");
        final GRN john = grnRegistry.parse("grn::::user:john");

        final GRN dashboard1 = grnRegistry.parse("grn::::dashboard:54e3deadbeefdeadbeef0000");
        final GRN dashboard2 = grnRegistry.parse("grn::::dashboard:54e3deadbeefdeadbeef0001");

        assertThat(dbService.getOwnersForTargets(ImmutableSet.of(dashboard1, dashboard2))).satisfies(result -> {
            assertThat(result.get(dashboard1)).containsExactlyInAnyOrder(jane);
            assertThat(result.get(dashboard2)).containsExactlyInAnyOrder(john);
        });
    }
}
