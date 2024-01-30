package org.mongojack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class JacksonDBCollectionTest {
    private MongoDBTestService mongoDBTestService;
    private MongoJackObjectMapperProvider mongoJackObjectMapperProvider;

    record Simple(@Id @ObjectId @JsonProperty("id") @Nullable String id,
                  @JsonProperty("name") String name) {}

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider mongoJackObjectMapperProvider) {
        this.mongoDBTestService = mongoDBTestService;
        this.mongoJackObjectMapperProvider = mongoJackObjectMapperProvider;
    }

    @Test
    void createIndex() {
        final var collection = jacksonCollection("simple", Simple.class);
        collection.createIndex(new BasicDBObject("name", 1));
        collection.createIndex(new BasicDBObject("_id", 1).append("name", 1));
        assertThat(mongoCollection("simple").listIndexes()).containsExactlyInAnyOrder(
                new Document("key", new Document("_id", 1))
                        .append("name", "_id_")
                        .append("v", 2),
                new Document("key", new Document("name", 1))
                        .append("name", "name_1")
                        .append("v", 2),
                new Document("key", new Document("_id", 1)
                        .append("name", 1))
                        .append("name", "_id_1_name_1")
                        .append("v", 2)
        );
    }

    @Test
    void createIndexWithOptions() {
        final var collection = jacksonCollection("simple", Simple.class);
        collection.createIndex(new BasicDBObject("name", 1), new BasicDBObject("sparse", true).append("unique", true));
        assertThat(mongoCollection("simple").listIndexes()).containsExactlyInAnyOrder(
                new Document("key", new Document("_id", 1))
                        .append("name", "_id_")
                        .append("v", 2),
                new Document("key", new Document("name", 1))
                        .append("name", "name_1")
                        .append("sparse", true)
                        .append("unique", true)
                        .append("v", 2)
        );
    }

    @Test
    void find() {
        final var collection = jacksonCollection("simple", Simple.class);
        final List<Simple> items = List.of(
                new Simple("000000000000000000000001", "foo"),
                new Simple("000000000000000000000002", "bar")
        );
        collection.insert(items);
        assertThat((Iterable<Simple>) collection.find()).containsAll(items);
        assertThat((Iterable<Simple>) collection.find(DBQuery.is("name", "bar")))
                .containsExactly(items.get(1));
        assertThat((Iterable<Simple>) collection.find(new BasicDBObjectBuilder().add("name", "bar").get()))
                .containsExactly(items.get(1));
    }

    @Test
    void findOneById() {
        final var collection = jacksonCollection("simple", Simple.class);
        final List<Simple> items = List.of(
                new Simple("000000000000000000000001", "foo"),
                new Simple("000000000000000000000002", "bar")
        );
        collection.insert(items);
        assertThat(collection.findOneById(items.get(1).id())).isEqualTo(items.get(1));
        assertThat(collection.findOneById(new org.bson.types.ObjectId().toHexString())).isNull();
    }

    @Test
    void findOne() {
        final var collection = jacksonCollection("simple", Simple.class);
        final List<Simple> items = List.of(
                new Simple("000000000000000000000001", "foo"),
                new Simple("000000000000000000000002", "bar")
        );
        collection.insert(items);
        assertThat(collection.findOne(new BasicDBObjectBuilder().add("name", "bar").get()))
                .isEqualTo(items.get(1));
        assertThat(collection.findOne(DBQuery.is("name", "bar"))).isEqualTo(items.get(1));
    }

    @Test
    void distinct() {
        final var collection = jacksonCollection("simple", Simple.class);
        final List<Simple> items = List.of(
                new Simple("000000000000000000000001", "foo"),
                new Simple("000000000000000000000002", "bar")
        );
        collection.insert(items);
    }

    @Test
    void count() {
        final var collection = jacksonCollection("simple", Simple.class);
        final List<Simple> items = List.of(
                new Simple("000000000000000000000001", "foo"),
                new Simple("000000000000000000000002", "bar")
        );
        collection.insert(items);
        assertThat(collection.count()).isEqualTo(2);
        assertThat(collection.count((DBObject) new BasicDBObject("name", "foo"))).isEqualTo(1);
    }

    @Test
    void save() {
        final var collection = jacksonCollection("simple", Simple.class);

        final var foo = new Simple("000000000000000000000001", "foo");

        final var saveFooResult = collection.save(foo);
        assertThat(saveFooResult.getSavedObject()).isEqualTo(foo);
        assertThat(collection.findOneById(saveFooResult.getSavedId())).isEqualTo(foo);

        final Simple updated = new Simple(foo.id(), "baz");
        final var saveUpdatedResult = collection.save(updated);
        assertThat(saveUpdatedResult.getSavedObject()).isEqualTo(updated);
        assertThat(collection.findOneById(foo.id())).isEqualTo(updated);

        final var saveBarResult = collection.save(new Simple(null, "bar"));
        assertThat(collection.findOneById(saveBarResult.getSavedId()).name()).isEqualTo("bar");

        assertThatThrownBy(() -> collection.save(foo, WriteConcern.W2))
                .isInstanceOf(MongoCommandException.class)
                .hasMessageContaining("cannot use 'w' > 1 when a host is not replicated");
    }

    @Test
    void remove() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");
        final var bar = new Simple("000000000000000000000002", "bar");

        collection.insert(List.of(foo, bar));
        assertThat(collection.remove(DBQuery.is("name", "foo")).getN()).isEqualTo(1);
        assertThat((Iterable<Simple>) collection.find()).containsExactly(bar);

        collection.insert(foo);
        assertThat(collection.remove(DBQuery.is("name", "foo")).getN()).isEqualTo(1);
        assertThat((Iterable<Simple>) collection.find()).containsExactly(bar);

        assertThatThrownBy(() -> collection.remove(DBQuery.empty(), WriteConcern.W2))
                .isInstanceOf(MongoCommandException.class)
                .hasMessageContaining("cannot use 'w' > 1 when a host is not replicated");
    }

    @Test
    void removeById() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");
        final var bar = new Simple("000000000000000000000002", "bar");

        collection.insert(List.of(foo, bar));
        assertThat(collection.removeById(foo.id()).getN()).isEqualTo(1);
        assertThat((Iterable<Simple>) collection.find()).containsExactly(bar);
    }

    @Test
    void updateWithObject() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");

        assertThatThrownBy(() -> collection.update(DBQuery.empty(), foo, false, false, WriteConcern.W2))
                .isInstanceOf(MongoCommandException.class)
                .hasMessageContaining("cannot use 'w' > 1 when a host is not replicated");

        collection.insert(foo);
        assertThat(collection.findOneById(foo.id())).isEqualTo(foo);

        final var updated = new Simple(foo.id(), "baz");

        final var updateResult = collection.update(DBQuery.is("_id", objectId(foo.id())), updated, false, false);
        assertThat(updateResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isTrue();
            assertThat(wr.getN()).isOne();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(foo.id())).isEqualTo(updated);

        final var bar = new Simple("000000000000000000000002", "bar");

        final var upsertAttemptResult = collection.update(DBQuery.is("_id", objectId(bar.id())), bar, false, false);
        assertThat(upsertAttemptResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isZero();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(bar.id())).isNull();

        final var upsertResult = collection.update(DBQuery.is("_id", objectId(bar.id())), bar, true, false);
        assertThat(upsertResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isOne();
            assertThat(wr.getUpsertedId()).isEqualTo(objectId(bar.id()));
        });
        assertThat(collection.findOneById(bar.id())).isEqualTo(bar);
    }

    @Test
    void updateWithObjectVariants() {
        final var collection = spy(jacksonCollection("simple", Simple.class));
        final var foo = new Simple("000000000000000000000001", "foo");

        final DBQuery.Query query = DBQuery.empty();
        collection.update(query, foo);
        verify(collection).update(eq(query), eq(foo), eq(false), eq(false));
        verify(collection).update(eq(query), eq(foo), eq(false), eq(false), eq(WriteConcern.ACKNOWLEDGED));
    }

    @Test
    void updateWithBson() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");

        collection.insert(foo);
        assertThat(collection.findOneById(foo.id())).isEqualTo(foo);

        final var updated = new Simple(foo.id(), "baz");

        final var updateResult = collection.update(DBQuery.is("_id", objectId(foo.id())),
                new DBUpdate.Builder().set("name", "baz"), false, false);
        assertThat(updateResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isTrue();
            assertThat(wr.getN()).isOne();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(foo.id())).isEqualTo(updated);

        final var bar = new Simple("000000000000000000000002", "bar");

        final var upsertAttemptResult = collection.update(DBQuery.is("_id", objectId(bar.id())),
                new DBUpdate.Builder().set("name", "bar"), false, false);
        assertThat(upsertAttemptResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isZero();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(bar.id())).isNull();

        final var upsertResult = collection.update(DBQuery.is("_id", objectId(bar.id())),
                new DBUpdate.Builder().set("name", "bar"), true, false);
        assertThat(upsertResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isOne();
            assertThat(wr.getUpsertedId()).isEqualTo(objectId(bar.id()));
        });
        assertThat(collection.findOneById(bar.id())).isEqualTo(bar);
    }

    @Test
    void updateWithBsonVariants() {
        final var collection = spy(jacksonCollection("simple", Simple.class));

        final DBQuery.Query query = DBQuery.empty();
        final DBUpdate.Builder update = new DBUpdate.Builder().set("name", "foo");

        collection.update(query, update);
        verify(collection).update(eq(query), eq(update), eq(false), eq(false));
    }

    @Test
    void updateByIdWithObject() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");

        final var upsertAttemptResult = collection.updateById(foo.id(), foo);
        assertThat(upsertAttemptResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isZero();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(foo.id())).isNull();

        collection.insert(foo);
        assertThat(collection.findOneById(foo.id())).isEqualTo(foo);

        final var updated = new Simple(foo.id(), "bar");
        final var updateResult = collection.updateById(foo.id(), updated);
        assertThat(updateResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isTrue();
            assertThat(wr.getN()).isOne();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(foo.id())).isEqualTo(updated);
    }

    @Test
    void updateByIdWithBson() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");

        // no upserts allowed, therefore this should be a no-op
        collection.updateById(foo.id(), new DBUpdate.Builder().set("name", "foo"));
        assertThat(collection.findOneById(foo.id())).isNull();

        collection.insert(foo);
        assertThat(collection.findOneById(foo.id())).isEqualTo(foo);

        final var updated = new Simple(foo.id(), "bar");
        collection.updateById(foo.id(), new DBUpdate.Builder().set("name", "bar"));
        assertThat(collection.findOneById(foo.id())).isEqualTo(updated);
    }

    @Test
    void updateMulti() {
        final var collection = jacksonCollection("simple", Simple.class);

        final var foo = new Simple("000000000000000000000001", "foo");
        final var bar = new Simple("000000000000000000000002", "bar");

        // no upserts allowed, therefore this should be a no-op
        var upsertAttemptResult = collection.updateMulti(
                DBQuery.in("_id", objectId(foo.id()), objectId(bar.id())), new DBUpdate.Builder().set("name", "baz"));
        assertThat(upsertAttemptResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isZero();
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.count()).isZero();

        collection.insert(List.of(foo, bar));
        assertThat((Iterable<Simple>) collection.find()).containsExactlyInAnyOrder(foo, bar);

        var updateResult = collection.updateMulti(
                DBQuery.in("_id", objectId(foo.id()), objectId(bar.id())), new DBUpdate.Builder().set("name", "baz"));
        assertThat(updateResult).isNotNull().satisfies(wr -> {
            assertThatThrownBy(wr::getSavedId).isInstanceOf(MongoException.class);
            assertThatThrownBy(wr::getSavedObject).isInstanceOf(MongoException.class);
            assertThat(wr.isUpdateOfExisting()).isTrue();
            assertThat(wr.getN()).isEqualTo(2);
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat((Iterable<Simple>) collection.find()).containsExactlyInAnyOrder(
                new Simple(foo.id(), "baz"), new Simple(bar.id(), "baz"));
    }

    @Test
    void insertObject() {
        final var collection = jacksonCollection("simple", Simple.class);

        assertThat(collection.count()).isZero();

        final var foo = new Simple("000000000000000000000001", "foo");
        var withExistingIdResult = collection.insert(foo);
        assertThat(withExistingIdResult).isNotNull().satisfies(wr -> {
            assertThat(wr.getSavedId()).isEqualTo(foo.id());
            assertThat(wr.getSavedObject()).isEqualTo(foo);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isEqualTo(0);
            assertThat(wr.getUpsertedId()).isNull();
        });
        assertThat(collection.findOneById(foo.id())).isEqualTo(foo);

        final var bar = new Simple(null, "bar");
        var withGeneratedIdResult = collection.insert(bar);
        assertThat(withGeneratedIdResult).isNotNull().satisfies(wr -> {
            assertThat(wr.getSavedId()).isNotNull();
            assertThat(wr.getSavedObject()).isEqualTo(new Simple(wr.getSavedId(), "bar"));
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isEqualTo(0);
            assertThat(wr.getUpsertedId()).isNull();
        });
    }

    @Test
    void insertList() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple("000000000000000000000001", "foo");
        final var bar = new Simple(null, "bar");

        assertThat(collection.insert(List.of(foo, bar))).isNotNull().satisfies(wr -> {
            assertThat(wr.getSavedId()).isEqualTo(foo.id());
            assertThat(wr.getSavedObject()).isEqualTo(foo);
            assertThat(wr.isUpdateOfExisting()).isFalse();
            assertThat(wr.getN()).isEqualTo(0);
            assertThat(wr.getUpsertedId()).isNull();
        });

        assertThat(collection.count()).isEqualTo(2);
        assertThat(collection.findOne(DBQuery.is("name", "foo"))).isNotNull();
        assertThat(collection.findOne(DBQuery.is("name", "bar"))).isNotNull();
    }

    @Test
    void getCount() {
        final var collection = jacksonCollection("simple", Simple.class);
        assertThat(collection.count()).isEqualTo(0);
        collection.insert(new Simple(null, "foo"));
        assertThat(collection.count()).isEqualTo(1);
        collection.insert(new Simple(null, "bar"));
        assertThat(collection.count()).isEqualTo(2);
        assertThat(collection.count((DBObject) new BasicDBObject(Map.of("name", Map.of("$in", List.of("foo", "bar"))))))
                .isEqualTo(2);
        assertThat(collection.count((DBObject) new BasicDBObject("name", "bar"))).isOne();
        collection.remove(DBQuery.empty());
        assertThat(collection.count()).isEqualTo(0);
    }

    @Test
    void findAndModifyWithBson() {
        final var collection = jacksonCollection("simple", Simple.class);

        var query = DBQuery.empty();
        var update = new DBUpdate.Builder().set("name", "foo");
        var returnNew = false;
        var upsert = false;
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert)).isNull();
        assertThat(collection.count()).isZero();

        upsert = true;
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert)).isNull();
        assertThat(collection.count()).isOne();

        update = new DBUpdate.Builder().set("name", "bar");
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert))
                .satisfies(item -> assertThat(item.name()).isEqualTo("foo"));
        assertThat(collection.count()).isOne();

        update = new DBUpdate.Builder().set("name", "baz");
        returnNew = true;
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert))
                .satisfies(item -> assertThat(item.name()).isEqualTo("baz"));
        assertThat(collection.count()).isOne();

        query = DBQuery.is("name", "foo");
        update = new DBUpdate.Builder().set("name", "foo");
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert))
                .satisfies(item -> assertThat(item.name()).isEqualTo("foo"));
        assertThat(collection.count()).isEqualTo(2);
    }

    @Test
    void findAndModifyWithBsonVariants() {
        final var collection = spy(jacksonCollection("simple", Simple.class));

        final var query = DBQuery.empty();
        final var update = new DBUpdate.Builder().set("name", "foo");

        collection.findAndModify(query, update);
        verify(collection).findAndModify(eq(query), isNull(), isNull(), eq(false), eq(update), eq(false), eq(false));
    }

    @Test
    void findAndModifyWithObject() {
        final var collection = jacksonCollection("simple", Simple.class);

        var query = DBQuery.empty();
        var update = new Simple(null, "foo");
        var returnNew = false;
        var upsert = false;
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert)).isNull();
        assertThat(collection.count()).isZero();

        upsert = true;
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert)).isNull();
        assertThat(collection.count()).isOne();

        update = new Simple(null, "bar");
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert))
                .satisfies(item -> assertThat(item.name()).isEqualTo("foo"));
        assertThat(collection.count()).isOne();

        update = new Simple(null, "baz");
        returnNew = true;
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert))
                .satisfies(item -> assertThat(item.name()).isEqualTo("baz"));
        assertThat(collection.count()).isOne();

        query = DBQuery.is("name", "foo");
        update = new Simple(null, "foo");
        assertThat(collection.findAndModify(query, null, null, false, update, returnNew, upsert))
                .satisfies(item -> assertThat(item.name()).isEqualTo("foo"));
        assertThat(collection.count()).isEqualTo(2);
    }

    @Test
    void findAndRemove() {
        final var collection = jacksonCollection("simple", Simple.class);

        final var foo = new Simple("000000000000000000000001", "foo");
        final var bar = new Simple("000000000000000000000002", "bar");

        collection.insert(List.of(foo, bar));

        assertThat(collection.findAndRemove(DBQuery.is("_id", objectId(foo.id())))).isEqualTo(foo);
        assertThat(collection.findAndRemove(DBQuery.is("_id", objectId(bar.id())))).isEqualTo(bar);
        assertThat(collection.count()).isZero();
    }

    @Test
    void dropIndexes() {
        final var collection = jacksonCollection("simple", Simple.class);

        collection.createIndex(new BasicDBObject("name", 1));
        collection.createIndex(new BasicDBObject("name", 1).append("_id", 1));

        assertThat(mongoCollection("simple").listIndexes()).extracting("name")
                .containsExactlyInAnyOrder("_id_", "name_1", "name_1__id_1");

        collection.dropIndexes();

        assertThat(mongoCollection("simple").listIndexes()).extracting("name")
                .containsExactlyInAnyOrder("_id_");
    }

    @Test
    void dropIndex() {
        final var collection = jacksonCollection("simple", Simple.class);

        collection.createIndex(new BasicDBObject("name", 1));
        collection.createIndex(new BasicDBObject("name", 1).append("_id", 1));

        assertThat(mongoCollection("simple").listIndexes()).extracting("name")
                .containsExactlyInAnyOrder("_id_", "name_1", "name_1__id_1");

        collection.dropIndex("name_1");

        assertThat(mongoCollection("simple").listIndexes()).extracting("name")
                .containsExactlyInAnyOrder("_id_", "name_1__id_1");

        collection.dropIndex(new BasicDBObject("name", 1).append("_id", 1));

        assertThat(mongoCollection("simple").listIndexes()).extracting("name")
                .containsExactlyInAnyOrder("_id_");
    }

    @Test
    void drop() {
        final var a = jacksonCollection("a", Simple.class);
        final var b = jacksonCollection("b", Simple.class);
        a.insert(new Simple(null, "foo"));
        b.insert(new Simple(null, "foo"));

        assertThat(mongoDBTestService.mongoDatabase().listCollections()).extracting("name")
                .containsExactlyInAnyOrder("a", "b");

        a.drop();

        assertThat(mongoDBTestService.mongoDatabase().listCollections()).extracting("name")
                .containsExactlyInAnyOrder("b");
    }

    @Test
    void getIndexInfo() {
        final var collection = jacksonCollection("simple", Simple.class);
        collection.createIndex(new BasicDBObject("name", 1));
        collection.createIndex(new BasicDBObject("_id", 1).append("name", 1));

        assertThat(collection.getIndexInfo()).containsExactlyInAnyOrder(
                new BasicDBObject("key", new Document("_id", 1))
                        .append("name", "_id_")
                        .append("v", 2),
                new BasicDBObject("key", new Document("name", 1))
                        .append("name", "name_1")
                        .append("v", 2),
                new BasicDBObject("key", new Document("_id", 1)
                        .append("name", 1))
                        .append("name", "_id_1_name_1")
                        .append("v", 2)
        );
    }

    @Test
    void getDbCollection() {
        final var collection = jacksonCollection("simple", Simple.class);

        assertThat(collection.getDbCollection()).isEqualTo(
                mongoDBTestService.mongoConnection().getDatabase().getCollection("simple"));
    }

    @Test
    void wasAcknowledged() {
        final var collection = jacksonCollection("simple", Simple.class);
        final var foo = new Simple(null, "foo");

        assertThat(collection.update(DBQuery.empty(), foo, false, false, WriteConcern.UNACKNOWLEDGED)
                .wasAcknowledged()).isFalse();
        assertThat(collection.update(DBQuery.empty(), foo, false, false, WriteConcern.ACKNOWLEDGED)
                .wasAcknowledged()).isTrue();
    }

    private <T> JacksonDBCollection<T, String> jacksonCollection(String collectionName, Class<T> valueType) {
        return JacksonDBCollection.wrap(
                mongoDBTestService.mongoConnection().getDatabase().getCollection(collectionName),
                valueType,
                String.class,
                mongoJackObjectMapperProvider.get()
        );
    }

    private MongoCollection<Document> mongoCollection(String collectionName) {
        return mongoDBTestService.mongoCollection(collectionName);
    }

    private org.bson.types.ObjectId objectId(String id) {
        return new org.bson.types.ObjectId(id);
    }
}
