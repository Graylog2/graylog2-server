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
package org.graylog2.security.hashing;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SHA1HashPasswordAlgorithmTest {
    private SHA1HashPasswordAlgorithm SHA1HashPasswordAlgorithm;

    @Before
    public void setUp() throws Exception {
        this.SHA1HashPasswordAlgorithm = new SHA1HashPasswordAlgorithm("passwordSecret");
    }

    @Test
    public void testSupports() throws Exception {
        assertThat(SHA1HashPasswordAlgorithm.supports("deadbeefaffedeadbeefdeadbeefaffedeadbeef")).isTrue();
        assertThat(SHA1HashPasswordAlgorithm.supports("{bcrypt}foobar")).isFalse();
        assertThat(SHA1HashPasswordAlgorithm.supports("{foobar}foobar")).isFalse();
    }

    @Test
    public void testHash() throws Exception {
        assertThat(SHA1HashPasswordAlgorithm.hash("foobar")).isEqualTo("baae906e6bbb37ca5033600fcb4824c98b0430fb");
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(SHA1HashPasswordAlgorithm.matches("baae906e6bbb37ca5033600fcb4824c98b0430fb", "foobar")).isTrue();
    }
}