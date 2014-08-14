/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.indexer.retention;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class RetentionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionStrategy.class);
    private final IndexManagement indexManagement;

    public enum Type {
        DELETE,
        CLOSE
    }

    protected RetentionStrategy(IndexManagement indexManagement) {
        this.indexManagement = indexManagement;
    }

    protected abstract void onMessage(Map<String, String> message);
    protected abstract boolean iterates();
    protected abstract Type getType();

    public void runStrategy(String indexName) {
        Stopwatch sw = Stopwatch.createStarted();

        if (iterates()) {
            // TODO: Run per message.
        }

        // Delete or close index.
        switch (getType()) {
            case DELETE:
                LOG.info("Strategy is deleting.");
                indexManagement.delete(indexName);
                break;
            case CLOSE:
                LOG.info("Strategy is closing.");
                indexManagement.close(indexName);
                break;
        }

        LOG.info("Finished index retention strategy [" + this.getClass().getCanonicalName() + "] for " +
                "index <{}> in {}ms.", indexName, sw.stop().elapsed(TimeUnit.MILLISECONDS));
    }

}
