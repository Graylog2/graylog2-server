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
package org.graylog2.utilities;

import com.google.common.util.concurrent.Service;

import java.util.concurrent.CountDownLatch;

/**
 * Counts down the given latch when the service has finished "starting", i.e. either it runs fine or failed during startup.
 *
 */
public class LatchUpdaterListener extends Service.Listener {
    private final CountDownLatch latch;

    public LatchUpdaterListener(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void running() {
        latch.countDown();
    }

    @Override
    public void failed(Service.State from, Throwable failure) {
        latch.countDown();
    }
}
