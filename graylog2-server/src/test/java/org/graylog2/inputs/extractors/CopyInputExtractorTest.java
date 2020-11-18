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
package org.graylog2.inputs.extractors;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Extractor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CopyInputExtractorTest extends AbstractExtractorTest {
    @Test
    public void testCopy() throws Extractor.ReservedFieldException, ConfigurationException {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "foo");

        CopyInputExtractor x = new CopyInputExtractor(metricRegistry, "bar", "bar", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", noConfig(), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertEquals("foo", msg.getField("our_result"));
        assertEquals("foo", msg.getField("somefield"));
    }
}
