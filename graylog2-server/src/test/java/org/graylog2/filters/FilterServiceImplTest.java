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
package org.graylog2.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.filters.blacklist.BlacklistPatternCondition;
import org.graylog2.filters.blacklist.FilterDescription;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class FilterServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private FilterService filterService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        this.filterService = new FilterServiceImpl(mongoRule.getMongoConnection(), new MongoJackObjectMapperProvider(objectMapper));
    }

    @Test
    @UsingDataSet(locations = "properDeserializationOfSubclasses.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testProperDeserializationOfSubclasses() throws Exception {
        final Set<FilterDescription> filterDescriptionSet = filterService.loadAll();
        assertThat(filterDescriptionSet).isNotNull().hasSize(1);

        final FilterDescription filterDescription = filterDescriptionSet.iterator().next();
        assertThat(filterDescription).isNotNull();
        assertThat(filterDescription).isInstanceOf(BlacklistPatternCondition.class);
    }
}