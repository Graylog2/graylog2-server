package org.graylog2.indexer.indexset.template;

import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.IndexSetDefaultTemplateConfigFactory;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSetDefaultTemplateServiceTest {

    private static final IndexSetDefaultTemplate INDEX_SET_DEFAULT_TEMPLATE = new IndexSetDefaultTemplate("1");
    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private IndexSetTemplateService indexSetTemplateService;
    @Mock
    private IndexSetDefaultTemplateConfigFactory indexSetDefaultTemplateConfigFactory;
    @Mock
    private AuditEventSender auditEventSender;
    @Mock
    IndexSetTemplate indexSetTemplate;

    @InjectMocks
    private IndexSetDefaultTemplateService underTest;


    @Test
    void testSetDefaultWithNotExistingTemplate() {
        assertThatThrownBy(() -> underTest.setDefault(INDEX_SET_DEFAULT_TEMPLATE, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testSetDefaultSuccessfully() throws Exception {
        when(indexSetTemplate.id()).thenReturn(INDEX_SET_DEFAULT_TEMPLATE.id());
        when(indexSetTemplate.title()).thenReturn("title");
        when(indexSetTemplateService.get(INDEX_SET_DEFAULT_TEMPLATE.id())).thenReturn(Optional.of(indexSetTemplate));

        underTest.setDefault(INDEX_SET_DEFAULT_TEMPLATE, "user");

        verify(clusterConfigService).write(INDEX_SET_DEFAULT_TEMPLATE);
    }

    @Test
    void testSetDefaultWithBuiltInTemplate() {
        when(indexSetTemplate.isBuiltIn()).thenReturn(true);
        when(indexSetTemplateService.get(INDEX_SET_DEFAULT_TEMPLATE.id())).thenReturn(Optional.of(indexSetTemplate));

        assertThatThrownBy(() -> underTest.setDefault(INDEX_SET_DEFAULT_TEMPLATE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("default");
    }
}
