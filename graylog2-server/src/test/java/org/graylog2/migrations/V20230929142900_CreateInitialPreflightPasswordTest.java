package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MongoDBExtension.class)
class V20230929142900_CreateInitialPreflightPasswordTest {

    private V20230929142900_CreateInitialPreflightPassword migration;
    private MongoCollection<Document> collection;


    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        collection = mongodb.mongoConnection().getMongoDatabase().getCollection("preflight");
        migration = new V20230929142900_CreateInitialPreflightPassword(mongodb.mongoConnection());
    }

    @Test
    @MongoDBFixtures({"V20230929142900_CreateInitialPreflightPassword/old_preflight_config_structure.json"})
    void testMigrateConfigCreatePassword() {

        Assertions.assertThat(collection.countDocuments()).isEqualTo(1); // the old format of configuration, one doc
        migration.upgrade();
        Assertions.assertThat(collection.countDocuments()).isEqualTo(2); // the old format of configuration, one doc

        String result = (String) collection.find(Filters.eq("type", "preflight_result")).first().get("value");
        Assertions.assertThat(result).isEqualTo("FINISHED");



        String password = (String) collection.find(Filters.eq("type", "preflight_password")).first().get("value");
        Assertions.assertThat(password).hasSizeGreaterThanOrEqualTo(PreflightConstants.INITIAL_PASSWORD_LENGTH);
    }
}
