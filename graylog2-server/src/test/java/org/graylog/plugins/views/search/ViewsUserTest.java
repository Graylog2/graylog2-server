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
package org.graylog.plugins.views.search;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ViewsUserTest {

    @Test
    public void exactUserOfSearchIsOwner() {
        ViewsUser sut = karl();

        final Search search = Search.builder().owner(sut.getName()).build();

        assertThat(sut.isOwnerOf(search)).isTrue();
    }

    @Test
    public void anyUserIsOwnerOfLegacySearchesWithoutOwner() {
        ViewsUser sut = karl();

        final Search search = Search.builder().build();

        assertThat(sut.isOwnerOf(search)).isTrue();
    }

    @Test
    public void usernameNotMatchingIsNotOwner() {
        ViewsUser sut = karl();

        final Search search = Search.builder().owner("friedrich").build();

        assertThat(sut.isOwnerOf(search)).isFalse();
    }

    private ViewsUser karl() {
        return new ViewsUser("karl", false, x -> true, x -> true, x -> true);
    }
}
