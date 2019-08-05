package org.graylog.plugins.views.migrations;

import com.lordofthejars.nosqlunit.annotation.ShouldMatchDataSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class V20190805115800_RemoveDashboardStateFromViewsTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private ClusterConfigService clusterConfigService;

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @ShouldMatchDataSet
    public void removesDashboardStateFromExistingViews() {
        final Migration migration = new V20190805115800_RemoveDashboardStateFromViews(clusterConfigService, mongoRule.getMongoConnection());

        migration.upgrade();

        final ArgumentCaptor<V20190805115800_RemoveDashboardStateFromViews.MigrationCompleted> argumentCaptor = ArgumentCaptor.forClass(V20190805115800_RemoveDashboardStateFromViews.MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().modifiedViewsCount()).isEqualTo(4);
    }
}
