package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShannonEntropyTest {
    @Test
    public void testEntropyCalcForChars() {
        assertEquals(0D, ShannonEntropy.calculateForChars("1111"));
        assertEquals(0D, ShannonEntropy.calculateForChars("5555555555"), 0.0D);
        assertEquals(0D, ShannonEntropy.calculateForChars("5555555555"), 0.0D);
        assertEquals(0.46899559358928133D, ShannonEntropy.calculateForChars("1555555555"));
        assertEquals(1.0D, ShannonEntropy.calculateForChars("1111155555"));
        assertEquals(3.3219280948873635D, ShannonEntropy.calculateForChars("1234567890"));
        assertEquals(5.1699250014423095D, ShannonEntropy.calculateForChars("1234567890qwertyuiopasdfghjklzxcvbnm"));
    }
}
