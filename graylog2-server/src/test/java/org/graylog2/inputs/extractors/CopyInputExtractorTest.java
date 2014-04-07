/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.extractors;

import com.google.common.collect.Lists;
import org.graylog2.ConfigurationException;
import org.graylog2.GraylogServerStub;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

public class CopyInputExtractorTest {

    @Test
    public void testCopy() throws Extractor.ReservedFieldException, ConfigurationException {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "foo");

        CopyInputExtractor x = new CopyInputExtractor("bar", "bar", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", noConfig(), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertEquals("foo", msg.getField("our_result"));
        assertEquals("foo", msg.getField("somefield"));
    }

    public static Map<String, Object> noConfig() {
        return new HashMap<String, Object>() {{
        }};
    }

    public static List<Converter> noConverters() {
        return Lists.newArrayList();
    }
}
