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
package org.graylog2.rest.models.messages.responses;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SimulationResultsTest {

    @Test
    public void testDecorationStats() {
        Map<String, Object> originalMessage = new HashMap<>();
        originalMessage.put("field1", true);
        originalMessage.put("field2", "value2");
        originalMessage.put("field3", "value3");
        originalMessage.put("field4", "value4");

        Map<String, Object> processedMessage = new HashMap<>();
        processedMessage.put("field2", "value2"); // field1 removed, field2 unchanged
        processedMessage.put("field3", 3); // changed
        processedMessage.put("field4", "changed"); // changed
        processedMessage.put("field5", "value5"); // added

        DecorationStats stats = DecorationStats.create(originalMessage, processedMessage);
        Map<String, Object> addedFields = stats.addedFields();
        Map<String, Object> removedFields = stats.removedFields();
        Map<String, Object> changedFields = stats.changedFields();
        assertThat(addedFields.size()).isEqualTo(1);
        Assert.assertTrue(addedFields.containsKey("field5"));
        assertThat(addedFields.get("field5")).isEqualTo("value5");
        assertThat(removedFields.size()).isEqualTo(1);
        Assert.assertTrue(removedFields.containsKey("field1"));
        assertThat(removedFields.get("field1")).isEqualTo(true);
        assertThat(changedFields.size()).isEqualTo(2);
        Assert.assertTrue(changedFields.containsKey("field3"));
        Assert.assertTrue(changedFields.containsKey("field4"));
        ChangedField field3 = (ChangedField) changedFields.get("field3");
        ChangedField field4 = (ChangedField) changedFields.get("field4");
        assertThat(field3.before()).isEqualTo("value3");
        assertThat(field3.after()).isEqualTo(3);
        assertThat(field4.before()).isEqualTo("value4");
        assertThat(field4.after()).isEqualTo("changed");
    }

}
