package integration;

import org.junit.Assume;
import org.junit.runners.model.Statement;

class IgnoreStatement extends Statement {
    private final String message;

    public IgnoreStatement(String message) {
        this.message = message;
    }

    @Override
    public void evaluate() throws Throwable {
        Assume.assumeTrue(this.message, false);
    }
}
