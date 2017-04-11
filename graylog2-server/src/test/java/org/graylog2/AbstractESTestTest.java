package org.graylog2;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
public class AbstractESTestTest extends AbstractESTest {
    public AbstractESTestTest() {
        assertThat(jestClient()).isNotNull();
    }

    @Test
    public void testIfJestClientIsNotNull() throws Exception {
        assertThat(jestClient()).isNotNull();
    }
}
