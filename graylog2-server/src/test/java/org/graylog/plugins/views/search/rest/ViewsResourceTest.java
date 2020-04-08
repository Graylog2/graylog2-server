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
package org.graylog.plugins.views.search.rest;

import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ViewsResourceTest {
    @Before
    public void setUpInjector() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Subject subject;

    @Mock
    private User currentUser;

    @Mock
    private ViewService viewService;

    @Mock
    private ViewSharingService viewSharingService;

    @Mock
    private ViewDTO view;

    @Mock
    private IsViewSharedForUser isViewSharedForUser;

    private ViewsResource viewsResource;

    class ViewsTestResource extends ViewsResource {
        ViewsTestResource(ViewService viewService, ViewSharingService viewSharingService, IsViewSharedForUser isViewSharedForUser) {
            super(viewService, viewSharingService, isViewSharedForUser);
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return currentUser;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.viewsResource = new ViewsTestResource(viewService, viewSharingService, isViewSharedForUser);
    }

    @Test
    public void creatingViewAddsCurrentUserAsOwner() throws Exception {
        final ViewDTO.Builder builder = mock(ViewDTO.Builder.class);

        when(view.toBuilder()).thenReturn(builder);
        when(view.type()).thenReturn(ViewDTO.Type.DASHBOARD);
        when(builder.owner(any())).thenReturn(builder);
        when(builder.build()).thenReturn(view);

        when(currentUser.getName()).thenReturn("basti");
        when(currentUser.isLocalAdmin()).thenReturn(true);

        when(subject.isPermitted("dashboards:create")).thenReturn(true);

        this.viewsResource.create(view);

        final ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder, times(1)).owner(ownerCaptor.capture());
        assertThat(ownerCaptor.getValue()).isEqualTo("basti");
    }

    @Test
    public void shouldNotCreateADashboardWithoutPermission() throws Exception {
        when(view.type()).thenReturn(ViewDTO.Type.DASHBOARD);

        when(subject.isPermitted("dashboards:create")).thenReturn(false);

        assertThatThrownBy(() -> this.viewsResource.create(view))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void shouldNotCreateASearchWithoutPermission() throws Exception {
        when(view.type()).thenReturn(ViewDTO.Type.SEARCH);

        when(subject.isPermitted("views:edit")).thenReturn(false);

        assertThatThrownBy(() -> this.viewsResource.create(view))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void invalidObjectIdReturnsViewNotFoundException() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        expectedException.expect(NotFoundException.class);
        this.viewsResource.get("invalid");
    }
}
