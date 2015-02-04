/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Size;
import org.joda.time.Duration;

import java.io.File;

public class KafkaJournalConfiguration {

    @Parameter(value = "message_journal_dir", required = true)
    @JsonProperty("directory")
    private File messageJournalDir = new File("data/journal");

    @Parameter("message_journal_segment_size")
    @JsonProperty("segment_size")
    private Size messageJournalSegmentSize = Size.megabytes(100l);

    @Parameter("message_journal_segment_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("segment_age")
    private Duration messageJournalSegmentAge = Duration.standardHours(1);

    @Parameter("message_journal_max_size")
    @JsonProperty("max_size")
    private Size messageJournalMaxSize = Size.gigabytes(5l);

    @Parameter("message_journal_max_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("max_age")
    private Duration messageJournalMaxAge = Duration.standardHours(12);

    @Parameter("message_journal_flush_interval")
    @JsonProperty("flush_interval")
    private long messageJournalFlushInterval = 1_000_000;

    @Parameter("message_journal_flush_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("flush_age")
    private Duration messageJournalFlushAge = Duration.standardMinutes(1);

    public File getMessageJournalDir() {
        return messageJournalDir;
    }

    public Size getMessageJournalSegmentSize() {
        return messageJournalSegmentSize;
    }

    public Duration getMessageJournalSegmentAge() {
        return messageJournalSegmentAge;
    }

    public Duration getMessageJournalMaxAge() {
        return messageJournalMaxAge;
    }

    public Size getMessageJournalMaxSize() {
        return messageJournalMaxSize;
    }

    public long getMessageJournalFlushInterval() {
        return messageJournalFlushInterval;
    }

    public Duration getMessageJournalFlushAge() {
        return messageJournalFlushAge;
    }
}
