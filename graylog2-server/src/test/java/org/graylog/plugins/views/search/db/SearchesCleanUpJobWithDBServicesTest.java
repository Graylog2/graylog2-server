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
package org.graylog.plugins.views.search.db;

import com.lordofthejars.nosqlunit.annotation.CustomComparisonStrategy;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.lordofthejars.nosqlunit.mongodb.MongoFlexibleComparisonStrategy;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.ValidatorModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(JukitoRunner.class)
@UseModules({ObjectMapperModule.class, ValidatorModule.class})
@CustomComparisonStrategy(comparisonStrategy = MongoFlexibleComparisonStrategy.class)
public class SearchesCleanUpJobWithDBServicesTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private SearchesCleanUpJob searchesCleanUpJob;
    private SearchDbService searchDbService;

    static class TestViewService extends ViewService {
        TestViewService(MongoConnection mongoConnection,
                        MongoJackObjectMapperProvider mapper,
                        ClusterConfigService clusterConfigService) {
            super(mongoConnection, mapper, clusterConfigService, view -> new ViewRequirements(Collections.emptySet(), view));
        }
    }

    @Before
    public void setup(MongoJackObjectMapperProvider mapperProvider) {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2018-07-03T13:37:42.000Z").getMillis());

        final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
        final ViewService viewService = new TestViewService(
                mongoRule.getMongoConnection(),
                mapperProvider,
                clusterConfigService
        );
        this.searchDbService = spy(
                new SearchDbService(
                        mongoRule.getMongoConnection(),
                        mapperProvider,
                        dto -> new SearchRequirements(Collections.emptySet(), dto)
                )
        );
        this.searchesCleanUpJob = new SearchesCleanUpJob(viewService, searchDbService, Duration.standardDays(4));
    }

    @After
    public void tearDown() throws Exception {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testForAllEmpty() {
        this.searchesCleanUpJob.doRun();

        verify(searchDbService, never()).delete(any());
    }

    @Test
    @UsingDataSet(locations = "mixedExpiredAndNonExpiredSearches.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testMixedExpiredAndNonExpiredSearches() {
        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(1)).delete(idCaptor.capture());

        assertThat(idCaptor.getAllValues()).containsExactly("5b3b44ca77196aa4679e4da0");
    }

    @Test
    @UsingDataSet(locations = "mixedExpiredNonExpiredReferencedAndNonReferencedSearches.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testMixedExpiredNonExpiredReferencedAndNonReferencedSearches() {
        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(2)).delete(idCaptor.capture());

        assertThat(idCaptor.getAllValues()).containsExactly("5b3b44ca77196aa4679e4da1", "5b3b44ca77196aa4679e4da2");
    }

}
