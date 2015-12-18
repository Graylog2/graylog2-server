package org.graylog2.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.bindings.providers.ServerObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.filters.blacklist.BlacklistPatternCondition;
import org.graylog2.filters.blacklist.FilterDescription;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.*;

public class FilterServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private FilterService filterService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ServerObjectMapperProvider().get();
        this.filterService = new FilterServiceImpl(mongoRule.getMongoConnection(), new MongoJackObjectMapperProvider(objectMapper));
    }

    @Test
    @UsingDataSet(locations = "properDeserializationOfSubclasses.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testProperDeserializationOfSubclasses() throws Exception {
        final Set<FilterDescription> filterDescriptionSet = filterService.loadAll();
        assertThat(filterDescriptionSet).isNotNull().hasSize(1);

        final FilterDescription filterDescription = filterDescriptionSet.iterator().next();
        assertThat(filterDescription).isNotNull();
        assertThat(filterDescription instanceof BlacklistPatternCondition);
    }
}