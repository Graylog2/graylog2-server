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
package org.graylog2.buffers.processors.fakestreams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.streams.StreamImpl;

import java.util.List;

public class FakeStream extends StreamImpl {
    private List<MessageOutput> outputs = Lists.newArrayList();

    public FakeStream(String title) {
        super(Maps.<String, Object>newHashMap());
    }

    public void addOutput(MessageOutput output) {
        outputs.add(output);
    }
}
