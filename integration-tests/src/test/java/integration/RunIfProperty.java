package integration;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RunIfProperty implements TestRule {
    private final String propertyName;

    public RunIfProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        final String propertyValue = System.getProperty(this.propertyName);
        if (propertyValue == null || !Boolean.valueOf(propertyValue)) {
            return new IgnoreStatement("Not running REST API integration tests. Add -Dgl2.integration.tests to run them.");
        } else {
            return base;
        }
    }
}
