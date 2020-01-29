/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
