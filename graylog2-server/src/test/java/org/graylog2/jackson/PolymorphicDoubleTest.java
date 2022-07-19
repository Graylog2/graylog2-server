package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.views.ViewServiceUsesViewRequirementsTest;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeUnitIntervalDTO;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongojack.JacksonDBCollection;

public class PolymorphicDoubleTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    JacksonDBCollection<ParentDTO, ObjectId> db;

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(ChildA.class),
            @JsonSubTypes.Type(ChildB.class)
    })
    public interface ParentDTO {
        String name();
    }

    public static class ChildA implements ParentDTO {
        @JsonProperty
        private Double d;
        public String name() {return "ChildA";}
        public double d(){return d;}
    }

    public static class ChildB implements ParentDTO {
        @JsonProperty
        private String s;
        public String name() {return "ChildB";}
        public String s() {return s;}
    }

    class MongoJackObjectMapperProviderForTest extends MongoJackObjectMapperProvider {
        public MongoJackObjectMapperProviderForTest(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        public ObjectMapper get() {
            return super.get().registerModule(new Jdk8Module());
        }
    }

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProviderForTest(new ObjectMapper());
        db = JacksonDBCollection.wrap(mongodb.mongoConnection().getDatabase().getCollection("polyCollection"),
                ParentDTO.class,
                ObjectId.class,
                objectMapperProvider.get(),
                null);
    }

    @Test
    @MongoDBFixtures("polymorphicDouble.json")
    public void readDouble() {
        final ParentDTO oneById = db.findOneById(new ObjectId("5ced4df1d6e8104c16f50e00"));
    }

}
