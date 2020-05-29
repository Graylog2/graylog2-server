/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
