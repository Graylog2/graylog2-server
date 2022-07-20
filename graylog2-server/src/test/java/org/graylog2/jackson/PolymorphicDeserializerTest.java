package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongojack.Id;
import org.mongojack.JacksonDBCollection;

import javax.annotation.Nullable;

public class PolymorphicDeserializerTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    JacksonDBCollection<Container, ObjectId> db;

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(ChildA.class),
            @JsonSubTypes.Type(ChildB.class)
    })
    public interface ParentDTO {
    }

    public static class ChildA implements ParentDTO {
        @JsonProperty
        private Double d;
    }

    public static class ChildB implements ParentDTO {
        @JsonProperty
        private String s;
    }

    public static class Container {
        @Id
        @org.mongojack.ObjectId
        String id;

        @JsonProperty("polyDto")
        ParentDTO polyDto;
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
                Container.class,
                ObjectId.class,
                objectMapperProvider.get(),
                null);
    }

    @Test
    @MongoDBFixtures("polymorphicDeserializer.json")
    public void readDouble() {
        final Container oneById = db.findOneById(new ObjectId("100000000000000000000000"));
    }

    @Test
    @MongoDBFixtures("polymorphicDeserializer.json")
    public void readString() {
        final Container oneById = db.findOneById(new ObjectId("200000000000000000000000"));
    }


}
