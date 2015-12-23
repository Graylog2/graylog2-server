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
package org.graylog2.shared.security;

import org.apache.shiro.subject.Subject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShiroPrincipalTest {
    @Test
    public void testGetNameWithNull() throws Exception {
        final Subject subject = mock(Subject.class);
        final ShiroPrincipal shiroPrincipal = new ShiroPrincipal(subject);

        assertThat(shiroPrincipal.getName()).isNull();
    }

    @Test
    public void testGetName() throws Exception {
        final Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn("test");
        final ShiroPrincipal shiroPrincipal = new ShiroPrincipal(subject);

        assertThat(shiroPrincipal.getName()).isEqualTo("test");
    }

    @Test
    public void testGetSubject() throws Exception {
        final Subject subject = mock(Subject.class);
        final ShiroPrincipal shiroPrincipal = new ShiroPrincipal(subject);

        assertThat(shiroPrincipal.getSubject()).isSameAs(subject);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSubject() throws Exception {
        new ShiroPrincipal(null);
    }
}