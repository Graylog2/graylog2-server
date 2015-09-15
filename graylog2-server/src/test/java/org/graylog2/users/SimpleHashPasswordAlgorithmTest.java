package org.graylog2.users;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleHashPasswordAlgorithmTest {
    private SimpleHashPasswordAlgorithm simpleHashPasswordAlgorithm;

    @Before
    public void setUp() throws Exception {
        this.simpleHashPasswordAlgorithm = new SimpleHashPasswordAlgorithm("passwordSecret");
    }

    @Test
    public void testSupports() throws Exception {
        assertThat(simpleHashPasswordAlgorithm.supports("foobar")).isTrue();
        assertThat(simpleHashPasswordAlgorithm.supports("{bcrypt}foobar")).isFalse();
        assertThat(simpleHashPasswordAlgorithm.supports("{foobar}foobar")).isFalse();
    }

    @Test
    public void testHash() throws Exception {
        assertThat(simpleHashPasswordAlgorithm.hash("foobar")).isEqualTo("baae906e6bbb37ca5033600fcb4824c98b0430fb");
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(simpleHashPasswordAlgorithm.matches("baae906e6bbb37ca5033600fcb4824c98b0430fb", "foobar"));

    }
}