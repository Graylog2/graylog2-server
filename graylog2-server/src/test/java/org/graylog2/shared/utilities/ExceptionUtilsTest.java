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
package org.graylog2.shared.utilities;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionUtilsTest {
    @Test
    public void formatMessageCause() {
        assertThat(ExceptionUtils.formatMessageCause(new Exception())).isNotBlank();
    }
    @Test
    public void getRootCauseMessage() {
        assertThat(ExceptionUtils.getRootCauseMessage(new Exception("cause1", new Exception("root")))).satisfies(m -> {
           assertThat(m).isNotBlank();
           assertThat(m).isEqualTo("root.");
        });
    }
    @Test
    public void getRootCauseOrMessage() {
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1", new Exception("root")))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("root.");
        });
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1"))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("cause1.");
        });
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1", new Exception("")))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("cause1.");
        });
    }

    @Test
    public void hasCauseOf_returnsTrueIfTheExceptionItselfIsSubtypeOfTheProvidedType() {
        assertThat(ExceptionUtils.hasCauseOf(new SocketTimeoutException("asdasd"), IOException.class)).isTrue();
        assertThat(ExceptionUtils.hasCauseOf(new IOException("asdasd"), IOException.class)).isTrue();
    }

    @Test
    public void hasCauseOf_returnsFalseIfNoCauseOfTheProvidedTypeFound() {
        assertThat(ExceptionUtils.hasCauseOf(
                new RuntimeException("parent", new RuntimeException("asdasd", new IllegalArgumentException())), IOException.class)).isFalse();
    }

    @Test
    public void hasCauseOf_returnsTrueIfTheCauseIsSubtypeOfTheProvidedType() {
        assertThat(ExceptionUtils.hasCauseOf(
                new RuntimeException("parent", new SocketTimeoutException("asdasd")), IOException.class)).isTrue();

        assertThat(ExceptionUtils.hasCauseOf(
                new RuntimeException("parent", new IOException("asdasd")), IOException.class)).isTrue();
    }

    @Test
    public void getShortenedStackTrace() {
        final IOException ioException = new IOException("io message");
        setTestStackTrace(ioException, "FileReader", "process", 42);
        final RuntimeException ex1 = new RuntimeException("socket message", ioException);
        setTestStackTrace(ex1, "TCPSocket", "read", 23);
        final RuntimeException ex0 = new RuntimeException("parent message", ex1);
        setTestStackTrace(ex0, "Main", "loop", 78);

        final String shortTrace = ExceptionUtils.getShortenedStackTrace(ex0);

        final String expected = "java.lang.RuntimeException: parent message\n" +
                "\tat Main.loop(Main.java:78)\n" +
                "\t... 1 more\n" +
                "Caused by: java.lang.RuntimeException: socket message\n" +
                "\tat TCPSocket.read(TCPSocket.java:23)\n" +
                "\t... 1 more\n" +
                "Caused by: java.io.IOException: io message\n" +
                "\tat FileReader.process(FileReader.java:42)\n" +
                "\t... 1 more\n";

        assertThat(shortTrace).isEqualTo(expected);
    }

    @Test
    public void getShortenedStackTraceMinimal() {
        final IOException ioException = new IOException("io message");
        final StackTraceElement traceElement = new StackTraceElement("FileReader", "process", "FileReader.java", 42);
        ioException.setStackTrace(new StackTraceElement[]{traceElement});

        final String shortTrace = ExceptionUtils.getShortenedStackTrace(ioException);

        final String expected = "java.io.IOException: io message\n" +
                "\tat FileReader.process(FileReader.java:42)\n";

        assertThat(shortTrace).isEqualTo(expected);
    }

    @Test
    public void getShortenedStackTrace2More() {
        final IOException ioException = new IOException("io message");
        final StackTraceElement traceElement = new StackTraceElement("FileReader", "process", "FileReader.java", 42);
        ioException.setStackTrace(new StackTraceElement[]{traceElement, traceElement, traceElement});

        final String shortTrace = ExceptionUtils.getShortenedStackTrace(ioException);

        final String expected = "java.io.IOException: io message\n" +
                "\tat FileReader.process(FileReader.java:42)\n" +
                "\t... 2 more\n";

        assertThat(shortTrace).isEqualTo(expected);
    }

    private void setTestStackTrace(Throwable t, String className, String method, int line) {
        final StackTraceElement traceElement = new StackTraceElement(className, method, className + ".java", line);
        t.setStackTrace(new StackTraceElement[]{traceElement, traceElement});
    }
}
