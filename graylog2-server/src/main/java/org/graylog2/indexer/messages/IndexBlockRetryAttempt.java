package org.graylog2.indexer.messages;

import com.github.rholder.retry.Attempt;
import org.apache.commons.lang.NotImplementedException;

import java.util.concurrent.ExecutionException;

/**
 * This is only used for reuse of a wait strategy
 */
public class IndexBlockRetryAttempt implements Attempt<Void> {

    private final long number;

    public IndexBlockRetryAttempt(long number) {
        this.number = number;
    }

    @Override
    public long getAttemptNumber() {
        return number;
    }

    @Override
    public long getDelaySinceFirstAttempt() {
        return 0;
    }

    @Override
    public Void get() throws ExecutionException {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasResult() {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasException() {
        throw new NotImplementedException();
    }

    @Override
    public Void getResult() throws IllegalStateException {
        throw new NotImplementedException();
    }

    @Override
    public Throwable getExceptionCause() throws IllegalStateException {
        throw new NotImplementedException();
    }
}
