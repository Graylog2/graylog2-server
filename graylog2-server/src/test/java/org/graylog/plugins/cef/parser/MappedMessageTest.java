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
package org.graylog.plugins.cef.parser;

import com.github.jcustenborder.cef.CEFParser;
import com.github.jcustenborder.cef.CEFParserFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappedMessageTest {
    private static final String HEADER = "CEF:0|SecureAuth|SecureAuth IdP|9.3|90030|Application - End request|4|";

    private final CEFParser parser = CEFParserFactory.create();

    private Logger logger;
    private Level originalLevel;
    private CapturingAppender appender;

    @BeforeEach
    void captureLogOutput() {
        appender = new CapturingAppender();
        appender.start();
        logger = (Logger) LogManager.getLogger(MappedMessage.class);
        originalLevel = logger.getLevel();
        logger.addAppender(appender);
        Configurator.setLevel(MappedMessage.class.getName(), Level.TRACE);
    }

    @AfterEach
    void releaseLogOutput() {
        logger.removeAppender(appender);
        appender.stop();
        Configurator.setLevel(MappedMessage.class.getName(), originalLevel);
    }

    private MappedMessage parse(String extensions, boolean useFullNames) {
        return new MappedMessage(parser.parse(HEADER + extensions), useFullNames);
    }

    private Map<String, Object> mapExtensions(String extensions) {
        return parse(extensions, false).mappedExtensions();
    }

    private List<LogEvent> warnings() {
        return appender.events.stream()
                .filter(event -> event.getLevel().isMoreSpecificThan(Level.WARN))
                .toList();
    }

    // An empty numeric value is a quirk of the sending device, not an operator problem, so it must
    // not produce a warning. Devices that send one on every message used to flood server.log.
    @Test
    void emptyNumericValueIsNotLoggedAsAWarning() {
        mapExtensions("cfp1= cs1=AUDIT src=10.0.0.1");

        assertTrue(warnings().isEmpty(), "Expected no warning for an empty numeric value, got: " + warnings());
    }

    // A value we genuinely cannot parse is worth reporting, but the stack trace is identical every
    // time and tells the operator nothing. The offending value does.
    @Test
    void malformedNumericValueIsLoggedWithoutAStackTrace() {
        mapExtensions("cfp1=abc cs1=AUDIT");

        final List<LogEvent> warnings = warnings();
        assertEquals(1, warnings.size(), "Expected exactly one warning, got: " + warnings);

        final LogEvent event = warnings.getFirst();
        assertNull(event.getThrown(), "Warning must not carry a throwable");

        final String message = event.getMessage().getFormattedMessage();
        assertTrue(message.contains("cfp1"), "Warning should name the field, got: " + message);
        assertTrue(message.contains("abc"), "Warning should include the offending value, got: " + message);
    }

    @Test
    void emptyNumericValueIsSkipped() {
        final MappedMessage message = parse("cfp1= cs1=AUDIT src=10.0.0.1", false);

        // The empty value does reach us from the CEF parser, it is dropped while mapping.
        assertEquals("", message.extensions().get("cfp1"));

        final Map<String, Object> fields = message.mappedExtensions();
        assertFalse(fields.containsKey("cfp1"));
        assertEquals("AUDIT", fields.get("cs1"));
        assertEquals("10.0.0.1", fields.get("src"));
    }

    @Test
    void emptyValueIsSkippedForEveryNumericType() {
        final Map<String, Object> fields = mapExtensions("cfp1= dlat= flexNumber1= cn1= type= cs1=AUDIT");

        assertFalse(fields.containsKey("cfp1"), "Float");
        assertFalse(fields.containsKey("dlat"), "Double");
        assertFalse(fields.containsKey("flexNumber1"), "Long");
        assertFalse(fields.containsKey("cn1"), "BigInteger");
        assertFalse(fields.containsKey("type"), "Integer");
        assertEquals("AUDIT", fields.get("cs1"));
        assertTrue(warnings().isEmpty(), "Expected no warnings, got: " + warnings());
    }

    @Test
    void validNumericValueIsConverted() {
        final Map<String, Object> fields = mapExtensions("cfp1=2.5 dlat=51.5 flexNumber1=7 cn1=42 type=1");

        assertEquals(2.5f, fields.get("cfp1"));
        assertEquals(51.5d, fields.get("dlat"));
        assertEquals(7L, fields.get("flexNumber1"));
        assertEquals(new BigInteger("42"), fields.get("cn1"));
        assertEquals(1, fields.get("type"));
    }

    // Empty values are dropped for numeric fields only. String and unmapped fields keep the empty
    // value so that existing has_field()/_exists_ searches keep behaving the same way.
    @Test
    void emptyStringValueIsRetained() {
        final Map<String, Object> fields = mapExtensions("cs1= cs2=AUDIT");

        assertTrue(fields.containsKey("cs1"));
        assertEquals("", fields.get("cs1"));
    }

    @Test
    void emptyUnmappedValueIsRetained() {
        final Map<String, Object> fields = mapExtensions("someVendorField= cs1=AUDIT");

        assertTrue(fields.containsKey("someVendorField"));
        assertEquals("", fields.get("someVendorField"));
    }

    // A label renames its field, so dropping an empty numeric value must not leave the label behind
    // as a field of its own.
    @Test
    void emptyNumericValueWithLabelIsSkipped() {
        final Map<String, Object> fields = mapExtensions("cfp1Label=RiskScore cfp1= cs1=AUDIT");

        assertFalse(fields.containsKey("cfp1"));
        assertFalse(fields.containsKey("cfp1Label"));
        assertFalse(fields.containsKey("RiskScore"));
        assertEquals("AUDIT", fields.get("cs1"));
    }

    @Test
    void emptyNumericValueIsSkippedWhenUsingFullNames() {
        final Map<String, Object> fields = parse("cfp1= cs1=AUDIT", true).mappedExtensions();

        assertFalse(fields.containsKey("deviceCustomFloatingPoint1"));
        assertFalse(fields.containsKey("cfp1"));
        assertEquals("AUDIT", fields.get("deviceCustomString1"));
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("capturing", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
