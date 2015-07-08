package org.graylog2.plugin.streams;

import org.junit.Test;

import static org.junit.Assert.*;

public class MatchingTypeTest {

    @Test
    public void testValueOfOrDefault() throws Exception {
        assertEquals(Stream.MatchingType.valueOfOrDefault("AND"), Stream.MatchingType.AND);
        assertEquals(Stream.MatchingType.valueOfOrDefault("OR"), Stream.MatchingType.OR);
        assertEquals(Stream.MatchingType.valueOfOrDefault(null), Stream.MatchingType.DEFAULT);
        assertEquals(Stream.MatchingType.valueOfOrDefault(""), Stream.MatchingType.DEFAULT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfOrDefaultThrowsExceptionForUnknownEnumName() {
        Stream.MatchingType.valueOfOrDefault("FOO");
    }
}