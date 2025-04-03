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
package org.graylog2.jmte;

import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class NamedDateRenderer implements NamedRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(NamedDateRenderer.class);
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String RENDERER_DESCRIPTION = "Use Java date format patterns like yyyy-MM-dd or HH:mm:ss";

    private DateTime convert(Object o) throws ParseException {
        if (o == null) {
            return null;
        }

        if (o instanceof DateTime) {
            return (DateTime) o;
        } else if (o instanceof Number) {
            long longValue = ((Number) o).longValue();
            return new DateTime(longValue);
        } else if (o instanceof String) {
            return DateTime.parse((String) o);
        }
        return null;
    }

    @Override
    public RenderFormatInfo getFormatInfo() {
        return new DateRendererFormatInfo(RENDERER_DESCRIPTION, DEFAULT_PATTERN);
    }

    @Override
    public String getName() {
        return "date";
    }

    @Override
    public Class<?>[] getSupportedClasses() {
        return new Class[] { DateTime.class, String.class, Integer.class, Long.class };
    }

    @Override
    public String render(Object o, String pattern, Locale locale, Map<String, Object> model) {
        String formatPattern = pattern != null ? pattern : DEFAULT_PATTERN;
        try {
            final DateTime value = convert(o);
            if (value != null) {
                final DateTimeFormatter formatter = locale != null
                        ? DateTimeFormat.forPattern(formatPattern).withLocale(locale)
                        : DateTimeFormat.forPattern(formatPattern);

                return formatter.print(value);
            } else {
                LOG.warn("Failed to convert [{}] to a date object for formatting.", o);
            }
        } catch (ParseException pe) {
            LOG.warn("Failed to convert [{}] to a date object for formatting.", o, pe);
        } catch (Exception e) {
            LOG.warn("Failed to format [{}] as a date string with format [{}]", o, formatPattern, e);
        }
        return o.toString();

    }
}

record DateRendererFormatInfo(String description, String defaultFormat) implements RenderFormatInfo {}
