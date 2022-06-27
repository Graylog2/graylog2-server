package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20220623125450_AddJobTypeToJobTriggerTest {

    private final V20220623125450_AddJobTypeToJobTrigger migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20220623125450_AddJobTypeToJobTriggerTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection(DBJobTriggerService.COLLECTION_NAME);
        migration = new V20220623125450_AddJobTypeToJobTrigger(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @BeforeEach
    public void setUp() {
        when(clusterConfigService.getOrDefault(any(), any())).thenReturn(V20220623125450_AddJobTypeToJobTrigger.MigrationCompleted.createEmpty());
    }

    @Test
    @MongoDBFixtures("V20220623125450_AddJobTypeToJobTriggerTest.json")
    void upgrade() {
        long before = collection.countDocuments(Filters.exists(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, false));
        Assertions.assertThat(before).isEqualTo(3);

        migration.upgrade();

        long after = collection.countDocuments(Filters.exists(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, true));
        Assertions.assertThat(after).isEqualTo(3);

        final long eventTypeCount = collection.countDocuments(Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, "event-processor-execution-v1"));
        Assertions.assertThat(eventTypeCount).isEqualTo(2);
        final long notificationTypeCount = collection.countDocuments(Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, "notification-execution-v1"));
        Assertions.assertThat(notificationTypeCount).isEqualTo(1);
    }
}
