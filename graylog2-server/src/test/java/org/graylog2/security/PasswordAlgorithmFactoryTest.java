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
package org.graylog2.security;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PasswordAlgorithmFactoryTest {

    @Mock
    private PasswordAlgorithm passwordAlgorithm1;
    @Mock
    private PasswordAlgorithm passwordAlgorithm2;

    private Map<String, PasswordAlgorithm> passwordAlgorithms;

    @Before
    public void setUp() throws Exception {
        this.passwordAlgorithms = ImmutableMap.<String, PasswordAlgorithm>builder()
                .put("algorithm1", passwordAlgorithm1)
                .put("algorithm2", passwordAlgorithm2)
                .build();
    }

    @Test
    public void testForPasswordShouldReturnFirstAlgorithm() throws Exception {
        when(passwordAlgorithm1.supports(anyString())).thenReturn(true);
        when(passwordAlgorithm2.supports(anyString())).thenReturn(false);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(passwordAlgorithms, passwordAlgorithm2);

        assertThat(passwordAlgorithmFactory.forPassword("foobar")).isEqualTo(passwordAlgorithm1);
    }

    @Test
    public void testForPasswordShouldReturnSecondAlgorithm() throws Exception {
        when(passwordAlgorithm1.supports(anyString())).thenReturn(false);
        when(passwordAlgorithm2.supports(anyString())).thenReturn(true);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(passwordAlgorithms, passwordAlgorithm2);

        assertThat(passwordAlgorithmFactory.forPassword("foobar")).isEqualTo(passwordAlgorithm2);
    }

    @Test
    public void testForPasswordShouldReturnNull() throws Exception {
        when(passwordAlgorithm1.supports(anyString())).thenReturn(false);
        when(passwordAlgorithm2.supports(anyString())).thenReturn(false);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(passwordAlgorithms, passwordAlgorithm2);

        assertThat(passwordAlgorithmFactory.forPassword("foobar")).isNull();
    }

    @Test
    public void testDefaultPasswordAlgorithm() throws Exception {
        final PasswordAlgorithm defaultPasswordAlgorithm = mock(PasswordAlgorithm.class);

        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.<String, PasswordAlgorithm>emptyMap(),
                defaultPasswordAlgorithm);

        assertThat(passwordAlgorithmFactory.defaultPasswordAlgorithm()).isEqualTo(defaultPasswordAlgorithm);
    }
}