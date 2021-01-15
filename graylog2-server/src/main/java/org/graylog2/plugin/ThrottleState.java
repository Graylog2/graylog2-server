/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin;

import com.google.common.base.MoreObjects;

public class ThrottleState {
    public long uncommittedJournalEntries;
    public long appendEventsPerSec;
    public long journalSize;
    public long journalSizeLimit;
    public long readEventsPerSec;
    public long processBufferCapacity;

    public ThrottleState() {
    }

    public ThrottleState(ThrottleState o) {
        this.uncommittedJournalEntries = o.uncommittedJournalEntries;
        this.appendEventsPerSec = o.appendEventsPerSec;
        this.journalSize = o.journalSize;
        this.journalSizeLimit = o.journalSizeLimit;
        this.readEventsPerSec = o.readEventsPerSec;
        this.processBufferCapacity = o.processBufferCapacity;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("uncommittedJournalEntries", uncommittedJournalEntries)
                .add("appendEventsPerSec", appendEventsPerSec)
                .add("readEventsPerSec", readEventsPerSec)
                .add("journalSize", journalSize)
                .add("journalSizeLimit", journalSizeLimit)
                .add("pbCapacity", processBufferCapacity)
                .toString();
    }
}
