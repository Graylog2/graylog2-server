package org.graylog.plugins.enterprise.search.rest;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.enterprise.search.views.QualifyingViewsService;
import org.graylog.plugins.enterprise.search.views.ViewParameterSummaryDTO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifyingViewsResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private QualifyingViewsService qualifyingViewsService;

    @Mock
    private Subject subject;

    class QualifyingViewsTestResource extends QualifyingViewsResource {
        private final Subject subject;

        public QualifyingViewsTestResource(QualifyingViewsService qualifyingViewsService, Subject subject) {
            super(qualifyingViewsService);
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return this.subject;
        }
    }

    private QualifyingViewsResource qualifyingViewsResource;

    @Before
    public void setUp() throws Exception {
        this.qualifyingViewsResource = new QualifyingViewsTestResource(qualifyingViewsService, subject);
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
        when(subject.isPermitted(EnterpriseSearchRestPermissions.VIEW_READ + ":view1")).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(subject.isPermitted(EnterpriseSearchRestPermissions.VIEW_READ + ":view2")).thenReturn(false);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsSomeViewsIfSomeArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(subject.isPermitted(EnterpriseSearchRestPermissions.VIEW_READ + ":view1")).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(subject.isPermitted(EnterpriseSearchRestPermissions.VIEW_READ + ":view2")).thenReturn(true);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).containsExactly(view2);
    }

    @Test
    public void returnsAllViewsIfAllArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(subject.isPermitted(EnterpriseSearchRestPermissions.VIEW_READ + ":view1")).thenReturn(true);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(subject.isPermitted(EnterpriseSearchRestPermissions.VIEW_READ + ":view2")).thenReturn(true);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter();

        assertThat(result).contains(view1, view2);
    }
}