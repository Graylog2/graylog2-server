package org.graylog.plugins.views.search.views;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewResolverDecoderTest {
    @Test
    public void testValidResolver() {
        final ViewResolverDecoder decoder = new ViewResolverDecoder("resolver:id");
        assertTrue(decoder.isResolverViewId());
        assertEquals("resolver", decoder.getResolverName());
        assertEquals("id", decoder.getViewId());
    }

    @Test
    public void testStandardView() {
        final ViewResolverDecoder decoder = new ViewResolverDecoder("62068954bd0cd7035876fcec");
        assertFalse(decoder.isResolverViewId());
        assertThatThrownBy(decoder::getResolverName)
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(decoder::getViewId)
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
