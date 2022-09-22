package org.graylog2.shared.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

    @Test
    void testContainsOnceMethodWorksCorrectly() {
        assertFalse(StringUtils.containsOnce("Abracadabra", null));
        assertFalse(StringUtils.containsOnce("Abracadabra", ""));
        assertFalse(StringUtils.containsOnce("Abracadabra", "Cucaracha!"));
        assertFalse(StringUtils.containsOnce("Abracadabra", "bra"));
        assertTrue(StringUtils.containsOnce("Abracadabra", "cadabra"));
        assertTrue(StringUtils.containsOnce("Abracadabra", "Abracadabra"));
    }
}
