package org.graylog.plugins.sidecar.configurations;

import org.graylog.plugins.sidecar.mapper.SidecarStatusMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SidecarStatusMapperTest {

    @Test
    public void replaceStringStatusSearchQuery() {
        final SidecarStatusMapper sidecarStatusMapper = new SidecarStatusMapper();

        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:running")).isEqualTo("status:0");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:unknown")).isEqualTo("status:1");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:failing")).isEqualTo("status:2");

        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:failing, status:running")).isEqualTo("status:2, status:0");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:failing, foobar")).isEqualTo("status:2, foobar");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("lol:wut, status:failing")).isEqualTo("lol:wut, status:2");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("lol:wut, status:failing, foobar")).isEqualTo("lol:wut, status:2, foobar");
    }
}