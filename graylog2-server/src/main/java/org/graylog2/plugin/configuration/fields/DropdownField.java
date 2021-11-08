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
package org.graylog2.plugin.configuration.fields;

import com.google.common.collect.Maps;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DropdownField extends AbstractConfigurationField {

    public static final String FIELD_TYPE = "dropdown";

    private String defaultValue;
    private final Map<String, String> values;
    private static final int MILLIS_PER_HOUR = 3600000;
    private static final int MILLIS_PER_MINUTE = 60000;

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, Optional isOptional) {
        this(name, humanName, defaultValue, values, null, isOptional);
    }

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional) {
        super(FIELD_TYPE, name, humanName, description, isOptional);
        this.defaultValue = defaultValue;
        this.values = values;
    }

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional, int position) {
        this(name, humanName, defaultValue, values, description, isOptional);
        this.position = position;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof String) {
            this.defaultValue = (String) defaultValue;
        }
    }

    @Override
    public Map<String, Map<String, String>> getAdditionalInformation() {
        Map<String, Map<String, String>> result = Maps.newHashMap();
        result.put("values", values);
        return result;
    }

    public static class ValueTemplates {

        public static Map<String, String> timeUnits() {
            Map<String, String> units = Maps.newHashMap();

            for (TimeUnit unit : TimeUnit.values()) {
                String human = unit.toString().toLowerCase(Locale.ENGLISH);
                units.put(unit.toString(), Character.toUpperCase(human.charAt(0)) + human.substring(1));
            }

            return units;
        }

        /**
         * Returns a sorted map of available Time Zones that is first sorted by UTC offset and then alphabetically
         *
         * @return map of sorted timezones
         */
        public static Map<String, String> timeZones() {
            Map<Integer, List<DateTimeZone>> offsetsAndTimezones = buildSortedTimeZoneMap();
            Map<String, String> timezones = new LinkedHashMap<>();
            for (List<DateTimeZone> dtzList : offsetsAndTimezones.values()) {
                for (DateTimeZone dtz : dtzList) {
                    timezones.put(dtz.getID(), buildTimeZoneDisplayName(dtz));
                }
            }
            return timezones;
        }

        public static Map<String, String> valueMapFromEnum(Class<? extends Enum> enumClass, Function<Enum, String> valueMapping) {
            return Arrays.stream(enumClass.getEnumConstants()).collect(Collectors.toMap(Enum::toString, valueMapping));
        }

        // Builds an ordered map of timezones sorted first by the timezone offset (keys of the TreeMap) and then
        // alphabetically the timezones in that specific offset. Iterating over the map and keys maintains that order
        @SuppressForbidden("Intentionally use system default timezone")
        private static Map<Integer, List<DateTimeZone>> buildSortedTimeZoneMap() {
            // get a sorted list of DateTimeZones based on their IDs
            List<DateTimeZone> dtzList = DateTimeZone.getAvailableIDs().stream()
                    .map(DateTimeZone::forID)
                    .sorted(Comparator.comparing(DateTimeZone::getID, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            // iterate over the sorted list and group each DateTimeZone by offset
            // TreeMap is an implementation of an OrderedMap where keys are ordered
            Map<Integer, List<DateTimeZone>> offsetsAndTimezones = new TreeMap<>();
            Instant now = new DateTime(DateTimeZone.getDefault()).toInstant();
            for (DateTimeZone dtz : dtzList) {
                int rawOffset = dtz.getOffset(now);
                List<DateTimeZone> timezonesForOffset = offsetsAndTimezones.getOrDefault(rawOffset, new ArrayList<>());
                timezonesForOffset.add(dtz);
                offsetsAndTimezones.put(rawOffset, timezonesForOffset);
            }

            return offsetsAndTimezones;
        }

        @SuppressForbidden("Intentionally use system default timezone")
        private static String buildTimeZoneDisplayName(DateTimeZone dtz) {
            Instant now = new DateTime(DateTimeZone.getDefault()).toInstant();
            int offset = dtz.getOffset(now);
            int offsetHours = offset / MILLIS_PER_HOUR;
            int remainderOffset = offset % MILLIS_PER_HOUR;
            if (remainderOffset < 0) {
                remainderOffset *= -1;
            }
            // some timezones have a half hour or three-quarter hour offsets included - handle them here
            int offsetMinutes = remainderOffset / MILLIS_PER_MINUTE;
            return String.format(Locale.getDefault(), "UTC%+03d:%02d - %s", offsetHours, offsetMinutes, dtz.getID());
        }
    }

}
