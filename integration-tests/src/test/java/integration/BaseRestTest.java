package integration;

import org.junit.ClassRule;
import org.junit.Rule;

public class BaseRestTest extends BaseRestTestHelper {
    @ClassRule public static RestAssuredSetupRule restAssuredSetupRule = new RestAssuredSetupRule();
    @Rule public RequiredVersionRule requiredVersionRule = new RequiredVersionRule(restAssuredSetupRule);
    @Rule public MongoDbSeedRule mongoDbSeedRule = new MongoDbSeedRule();
}
