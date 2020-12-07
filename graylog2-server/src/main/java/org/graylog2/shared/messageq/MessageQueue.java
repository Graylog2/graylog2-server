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
package org.graylog2.shared.messageq;

import com.google.common.util.concurrent.Service;

import javax.annotation.Nullable;

public interface MessageQueue extends Service {

    interface Entry {
        Object commitId();

        /**
         * The journal entry ID.
         * @return the ID value
         */
        byte[] id();

        /**
         * The journal entry key. This is supposed to be the shard key for journal implementations that support it.
         * @return the key value
         */
        @Nullable
        byte[] key();

        /**
         * The journal entry value.
         * @return the vale
         */
        byte[] value();

        /**
         * This is the event time in milliseconds of the entry.
         * @return the event time in milliseconds
         */
        long timestamp();
    }
}
