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
package org.graylog2.plugin;

import com.github.joschi.jadconfig.Parameter;
import org.joda.time.Duration;

public class KafkaJournalConfiguration {

    @Parameter("message_journal_dir")
    private String messageJournalDir = "journal";

    @Parameter("message_journal_segment_size")
    private int messageJournalSegmentSize = 1024 * 1024 * 100; // 100 MB

    @Parameter("message_journal_max_size")
    private long messageJournalMaxSize = 1024 * 1024 * 1024 * 5l; // 5 GB

    @Parameter("message_journal_max_age")
    private Duration messageJournalMaxAge = Duration.standardHours(12);

    public String getMessageJournalDir() {
        return messageJournalDir;
    }

    public int getMessageJournalSegmentSize() {
        return messageJournalSegmentSize;
    }

    public Duration getMessageJournalMaxAge() {
        return messageJournalMaxAge;
    }

    public long getMessageJournalMaxSize() {
        return messageJournalMaxSize;
    }

}
