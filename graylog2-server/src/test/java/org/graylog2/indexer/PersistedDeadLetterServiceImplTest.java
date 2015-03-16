package org.graylog2.indexer;

import com.lordofthejars.nosqlunit.annotation.ShouldMatchDataSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.database.MongoConnectionRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.junit.Assert.*;

public class PersistedDeadLetterServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private PersistedDeadLetterService persistedDeadLetterService;

    @Before
    public void setupService() {
        this.persistedDeadLetterService = new PersistedDeadLetterServiceImpl(mongoRule.getMongoConnection());
    }

    @Test
    @UsingDataSet(locations = "emptyCollection.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void emptyCollection() throws Exception {
        assertEquals("Collection should be empty", 0, persistedDeadLetterService.count());
    }

    @Test
    @UsingDataSet(locations = "singleDocument.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void singleDocument() throws Exception {
        final long documentCount = persistedDeadLetterService.count();
        assertEquals("Collection should contain exactly one document", 1, documentCount);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @ShouldMatchDataSet(location = "singleDocument.json")
    public void saveSingleDocument() throws Exception {
        final PersistedDeadLetter deadLetter = persistedDeadLetterService.create("54e3deadbeefdeadbeefaffe", "54f9deadbeefdeadbeefaffe", DateTime.parse("2015-03-14T15:09:26.540Z"), new HashMap<String, Object>());
        persistedDeadLetterService.save(deadLetter);

        assertEquals("Collection should contain exactly one document", 1, persistedDeadLetterService.count());
    }
}