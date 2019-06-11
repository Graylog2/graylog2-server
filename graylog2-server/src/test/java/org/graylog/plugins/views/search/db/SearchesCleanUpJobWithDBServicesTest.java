package org.graylog.plugins.views.search.db;

import com.lordofthejars.nosqlunit.annotation.CustomComparisonStrategy;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.lordofthejars.nosqlunit.mongodb.MongoFlexibleComparisonStrategy;
import org.graylog.plugins.database.MongoConnectionRule;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchesCleanUpJob;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.ValidatorModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
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
        final ViewSharingService viewSharingService = mock(ViewSharingService.class);
        final IsViewSharedForUser isViewSharedForUser = mock(IsViewSharedForUser.class);
        this.searchDbService = spy(
                new SearchDbService(
                        mongoRule.getMongoConnection(),
                        mapperProvider,
                        viewService,
                        viewSharingService,
                        isViewSharedForUser,
                        dto -> new SearchRequirements(Collections.emptySet(), dto)
                )
        );
        this.searchesCleanUpJob = new SearchesCleanUpJob(viewService, searchDbService, Duration.standardDays(4));
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
