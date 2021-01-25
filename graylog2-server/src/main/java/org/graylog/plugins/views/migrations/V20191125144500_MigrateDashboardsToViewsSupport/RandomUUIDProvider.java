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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.eaio.uuid.UUIDGen;

import javax.inject.Inject;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RandomUUIDProvider {
    private final AtomicLong date;
    private final long clockSeqAndNode;

    @Inject
    public RandomUUIDProvider(Date date) {
        this(date, UUIDGen.getClockSeqAndNode());
    }

    public RandomUUIDProvider(Date date, long clockSeqAndNode) {
        this.date = new AtomicLong(date.getTime());
        this.clockSeqAndNode = clockSeqAndNode;
    }

    public String get() {
        return new UUID(date.getAndIncrement(), this.clockSeqAndNode).toString();
    }
}
