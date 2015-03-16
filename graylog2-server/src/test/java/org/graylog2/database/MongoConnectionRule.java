package org.graylog2.database;

import com.lordofthejars.nosqlunit.core.NoSqlAssertionError;
import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import static com.lordofthejars.nosqlunit.mongodb.MongoDbRule.MongoDbRuleBuilder.newMongoDbRule;

public class MongoConnectionRule implements MethodRule {
    private final String dbName;
    private final MongoDbRule mongoDbRule;

    public MongoConnectionRule(String dbName) {
        this.dbName = dbName;
        this.mongoDbRule = newMongoDbRule().defaultEmbeddedMongoDb(dbName);
    }

    public static MongoConnectionRule build(String dbName) {
        return new MongoConnectionRule(dbName);
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return mongoDbRule.apply(base, method, target);
    }

    public MongoConnection getMongoConnection() {
        return new MongoConnectionForTests(mongoDbRule.getDatabaseOperation().connectionManager(), dbName);
    }
}
