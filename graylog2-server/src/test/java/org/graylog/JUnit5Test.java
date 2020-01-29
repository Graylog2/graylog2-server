package org.graylog;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


class JUnit5Test {

    private static final Logger LOG = LoggerFactory.getLogger(JUnit5Test.class);

    @BeforeAll
    static void beforeAll() {
        LOG.info("BEFORE ALL");
    }

    @BeforeEach
    void setUp() {
        LOG.info("SETUP");
    }

    @AfterEach
    void tearDown() {
        LOG.info("TEARDOWN");
    }

    @AfterAll
    static void afterAll() {
        LOG.info("AFTER ALL");
    }

    @DisplayName("Expected failure (╯°□°)╯︵ ┻━┻")
    @Test
    void someTest() {
        LOG.info("TEST");
        fail("junit 5 test failed on purpose");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void aParameterizedTest(int aNumber) {
        assertThat(aNumber).isGreaterThan(0);
    }

    @Disabled
    @Test
    void disabledTest() {
        fail("This should not have run");
    }
}
