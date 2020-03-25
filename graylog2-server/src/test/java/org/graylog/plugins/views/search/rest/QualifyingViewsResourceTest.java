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

import com.google.common.collect.ImmutableList;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.views.QualifyingViewsService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewParameterSummaryDTO;
import org.graylog.plugins.views.search.views.sharing.AllUsersOfInstance;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharing;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifyingViewsResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private QualifyingViewsService qualifyingViewsService;

    @Mock
    private ViewSharingService viewSharingService;

    @Mock
    private IsViewSharedForUser isViewSharedForUser;

    @Mock
    private Subject subject;

    @Mock
    private User currentUser;

    class QualifyingViewsTestResource extends QualifyingViewsResource {
        private final Subject subject;

        QualifyingViewsTestResource(QualifyingViewsService qualifyingViewsService, Subject subject, IsViewSharedForUser isViewSharedForUser) {
            super(qualifyingViewsService, viewSharingService, isViewSharedForUser);
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return this.subject;
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return currentUser;
        }
    }

    private QualifyingViewsResource qualifyingViewsResource;

    @Before
    public void setUp() throws Exception {
        this.qualifyingViewsResource = new QualifyingViewsTestResource(qualifyingViewsService, subject, isViewSharedForUser);
    }

    @Test
    public void returnsNoViewsIfNoneArePresent() {
        when(qualifyingViewsService.forValue()).thenReturn(Collections.emptyList());

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsNoViewsIfNoneArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view1")).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(view2.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view2")).thenReturn(false);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsSomeViewsIfSomeArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view1")).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(view2.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view2")).thenReturn(true);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).containsExactly(view2);
    }

    @Test
    public void returnsAllViewsIfAllArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view1")).thenReturn(true);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view2")).thenReturn(true);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).contains(view1, view2);
    }

    @Test
    public void returnsViewIfNotPermittedButSharedWithUser() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view1")).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(view2.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(subject.isPermitted(ViewsRestPermissions.VIEW_READ + ":view2")).thenReturn(false);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));
        final ViewSharing allUsersOfInstance = AllUsersOfInstance.create("view1");
        when(viewSharingService.forView("view1")).thenReturn(Optional.of(allUsersOfInstance));
        when(isViewSharedForUser.isAllowedToSee(currentUser, allUsersOfInstance)).thenReturn(true);

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).containsExactly(view1);
    }
}
