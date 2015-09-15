package org.graylog2.users;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BCryptPasswordAlgorithmTest {
    private BCryptPasswordAlgorithm bCryptPasswordAlgorithm;

    @Before
    public void setUp() throws Exception {
        this.bCryptPasswordAlgorithm = new BCryptPasswordAlgorithm();
    }

    @Test
    public void testSupports() throws Exception {
        assertThat(bCryptPasswordAlgorithm.supports("foobar")).isFalse();
        assertThat(bCryptPasswordAlgorithm.supports("{bcrypt}foobar")).isTrue();
        assertThat(bCryptPasswordAlgorithm.supports("{bcrypt}foobar{salt}pepper")).isTrue();
        assertThat(bCryptPasswordAlgorithm.supports("{foobar}foobar")).isFalse();
    }

    @Test
    public void testHash() throws Exception {
        final String clearTextPassword = "foobar";
        final String hashedPassword = bCryptPasswordAlgorithm.hash(clearTextPassword);

        assertThat(hashedPassword)
                .isNotEmpty()
                .startsWith("{bcrypt}")
                .contains("{salt}");

        assertThat(bCryptPasswordAlgorithm.matches(hashedPassword, clearTextPassword)).isTrue();
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(bCryptPasswordAlgorithm.matches("{bcrypt}$2a$12$8lRgZZTqRWO2.Mk37Gl7re7uD0QoDkdSF/UtFfVx0BqqgI23/jtkO{salt}$2a$12$8lRgZZTqRWO2.Mk37Gl7re", "foobar")).isTrue();
    }
}