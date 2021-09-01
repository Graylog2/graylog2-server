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
package org.graylog2.logmessage;

import com.codahale.metrics.Meter;
import org.graylog.failure.FailureCause;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageTest {

    @Test
    public void testIdGetsSet() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        assertNotNull(lm.getId());
        assertFalse(lm.getId().isEmpty());
    }

    @Test
    public void testIsCompleteSucceeds() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        assertTrue(lm.isComplete());
    }

    @Test
    public void testIsCompleteFails() {
        Message lm = new Message("foo", null, Tools.nowUTC());
        assertTrue(lm.isComplete());

        lm = new Message("foo", "", Tools.nowUTC());
        assertTrue(lm.isComplete());

        lm = new Message(null, "bar", Tools.nowUTC());
        assertFalse(lm.isComplete());

        lm = new Message("", "bar", Tools.nowUTC());
        assertFalse(lm.isComplete());

        lm = new Message("", "", Tools.nowUTC());
        assertFalse(lm.isComplete());

        lm = new Message(null, null, Tools.nowUTC());
        assertFalse(lm.isComplete());
    }

    @Test
    public void testAddField() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        lm.addField("ohai", "thar");
        assertEquals("thar", lm.getField("ohai"));
    }

    @Test
    public void testAddFieldsWithMap() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        lm.addField("ohai", "hai");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("lol", "wut");
        map.put("aha", "pipes");

        lm.addFields(map);
        assertEquals(7, lm.getFieldCount());
        assertEquals("wut", lm.getField("lol"));
        assertEquals("pipes", lm.getField("aha"));
        assertEquals("hai", lm.getField("ohai"));
    }

    @Test
    public void testRemoveField() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        lm.addField("something", "foo");
        lm.addField("something_else", "bar");

        lm.removeField("something_else");

        assertEquals(5, lm.getFieldCount());
        assertEquals("foo", lm.getField("something"));
    }

    @Test
    public void testRemoveFieldWithNonExistentKey() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        lm.addField("something", "foo");
        lm.addField("something_else", "bar");

        lm.removeField("LOLIDONTEXIST");

        assertEquals(6, lm.getFieldCount());
    }

    @Test
    public void testRemoveFieldDoesNotDeleteReservedFields() {
        DateTime time = Tools.nowUTC();
        Message lm = new Message("foo", "bar", time);
        lm.removeField("source");
        lm.removeField("timestamp");
        lm.removeField("_id");

        assertTrue(lm.isComplete());
        assertEquals("foo", lm.getField("message"));
        assertEquals("bar", lm.getField("source"));
        assertEquals(time, lm.getField("timestamp"));
        assertEquals(4, lm.getFieldCount());
    }

    @Test
    public void testToString() {
        Message lm = new Message("foo", "bar", Tools.nowUTC());
        lm.toString();
        // Fine if it does not crash.
    }

    @Test
    public void addProcessingError_appendsWithEachCall() {
        final Message msg = new Message(new ImmutableMap.Builder<String, Object>()
                .put(Message.FIELD_ID, "msg-id")
                .put(Message.FIELD_TIMESTAMP, Tools.buildElasticSearchTimeFormat(Tools.nowUTC()))
                .build());

        final FailureCause cause1 = () -> "Cause 1";
        final FailureCause cause2 = () -> "Cause 2";

        msg.addProcessingError(new Message.ProcessingError(cause1, "Failure Message #1", "Failure Details #1"));

        assertThat(msg.processingErrors())
                .containsExactly(new Message.ProcessingError(cause1, "Failure Message #1", "Failure Details #1"));

        msg.addProcessingError(new Message.ProcessingError(cause2, "Failure Message #2", "Failure Details #2"));

        assertThat(msg.processingErrors())
                .containsExactly(
                        new Message.ProcessingError(cause1, "Failure Message #1", "Failure Details #1"),
                        new Message.ProcessingError(cause2, "Failure Message #2", "Failure Details #2"));
    }

    @Test
    public void processingErrors_returnImmutableList() {
        final Message msg = new Message(new ImmutableMap.Builder<String, Object>()
                .put(Message.FIELD_ID, "msg-id")
                .put(Message.FIELD_TIMESTAMP, Tools.buildElasticSearchTimeFormat(Tools.nowUTC()))
                .build());

        msg.addProcessingError(new Message.ProcessingError(() -> "Cause", "Failure Message #1", "Failure Details #1"));

        assertThat(msg.processingErrors()).hasSize(1);

        assertThatCode(() -> msg.processingErrors().add(new Message.ProcessingError(() -> "Cause 2", "Failure Message #2", "Failure Details #2")))
                .isInstanceOf(Exception.class);

        assertThat(msg.processingErrors()).hasSize(1);
    }

    @Test
    public void toElasticSearchObject_processingErrorDetailsAreJoinedInOneStringAndReturnedInProcessingErrorField() {
        // given
        final Message msg = new Message(new ImmutableMap.Builder<String, Object>()
                .put(Message.FIELD_ID, "msg-id")
                .put(Message.FIELD_TIMESTAMP, Tools.buildElasticSearchTimeFormat(Tools.nowUTC()))
                .build());

        msg.addProcessingError(new Message.ProcessingError(
                () -> "Cause 1", "Failure Message #1", "Failure Details #1"
        ));

        msg.addProcessingError(new Message.ProcessingError(
                () -> "Cause 2", "Failure Message #2", "Failure Details #2"
        ));

        // when
        final Map<String, Object> esObject = msg.toElasticSearchObject(new ObjectMapperProvider().get(), new Meter());

        // then
        assertThat(esObject.get(Message.FIELD_GL2_PROCESSING_ERROR))
                .isEqualTo("Failure Details #1, Failure Details #2");
    }
}
