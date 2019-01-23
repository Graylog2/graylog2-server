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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Beats2CodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private Configuration configuration;
    private Beats2Codec codec;

    @Before
    public void setUp() throws Exception {
        configuration = new Configuration(Collections.singletonMap("no_beats_prefix", false));
        codec = new Beats2Codec(configuration, objectMapper);
    }

    @Test
    public void decodeReturnsNullIfPayloadCouldNotBeDecoded() throws Exception {
        assertThat(codec.decode(new RawMessage(new byte[0]))).isNull();
    }

    @Test
    public void decodeMessagesHandlesFilebeatMessagesWithoutPrefix() throws Exception {
        configuration = new Configuration(Collections.singletonMap("no_beats_prefix", true));
        codec = new Beats2Codec(configuration, objectMapper);

        final Message message = codec.decode(messageFromJson("filebeat.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("TEST");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("filebeat");
        assertThat(message.getField("source")).isEqualTo("/tmp/test.log");
        assertThat(message.getField("input_type")).isEqualTo("log");
        assertThat(message.getField("count")).isEqualTo(1);
        assertThat(message.getField("offset")).isEqualTo(0);
        assertThat(message.getField(Message.FIELD_GL2_SOURCE_COLLECTOR)).isEqualTo("1234-5678-1234-5678");
        assertThat(message.getField("filebeat_" + Message.FIELD_GL2_SOURCE_COLLECTOR)).isNull();
        @SuppressWarnings("unchecked") final List<String> tags = (List<String>) message.getField("tags");
        assertThat(tags).containsOnly("foobar", "test");
    }

    @Test
    public void decodeMessagesHandlesFilebeatMessages() throws Exception {
        final Message message = codec.decode(messageFromJson("filebeat.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("TEST");
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("filebeat");
        assertThat(message.getField("filebeat_source")).isEqualTo("/tmp/test.log");
        assertThat(message.getField("filebeat_input_type")).isEqualTo("log");
        assertThat(message.getField("filebeat_count")).isEqualTo(1);
        assertThat(message.getField("filebeat_offset")).isEqualTo(0);
        assertThat(message.getField(Message.FIELD_GL2_SOURCE_COLLECTOR)).isEqualTo("1234-5678-1234-5678");
        assertThat(message.getField("filebeat_message")).isNull(); //should not be duplicated from "message"
        assertThat(message.getField("filebeat_" + Message.FIELD_GL2_SOURCE_COLLECTOR)).isNull();
        @SuppressWarnings("unchecked") final List<String> tags = (List<String>) message.getField("filebeat_tags");
        assertThat(tags).containsOnly("foobar", "test");
    }

    @Test
    public void decodeMessagesHandlesPacketbeatMessages() throws Exception {
        final Message message = codec.decode(messageFromJson("packetbeat-dns.json"));
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("packetbeat");
        assertThat(message.getField("packetbeat_type")).isEqualTo("dns");
        assertThat(message.getField("packetbeat_status")).isEqualTo("OK");
        assertThat(message.getField("packetbeat_method")).isEqualTo("QUERY");
        assertThat(message.getField("packetbeat_dns_answers_0_type")).isEqualTo("A");
        assertThat(message.getField("packetbeat_dns_flags_recursion_allowed")).isEqualTo(true);
    }

    @Test
    public void decodeMessagesHandlesTopbeatMessages() throws Exception {
        final Message message = codec.decode(messageFromJson("topbeat-system.json"));
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("topbeat");
        assertThat(message.getField("topbeat_type")).isEqualTo("system");
    }

    @Test
    public void decodeMessagesHandlesWinlogbeatMessages() throws Exception {
        final Message message = codec.decode(messageFromJson("winlogbeat.json"));
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 11, 24, 12, 13, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("winlogbeat");
        assertThat(message.getField("winlogbeat_type")).isEqualTo("wineventlog");
        assertThat(message.getField("winlogbeat_level")).isEqualTo("Information");
        assertThat(message.getField("winlogbeat_event_id")).isEqualTo(5024);
        assertThat(message.getField("winlogbeat_process_id")).isEqualTo(500);
        assertThat(message.getField("winlogbeat_log_name")).isEqualTo("Security");
    }

    @Test
    public void decodeMessagesHandleGenericBeatMessages() throws Exception {
        final Message message = codec.decode(messageFromJson("generic.json"));
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
    }

    @Test
    public void decodeMessagesHandleGenericBeatMessagesWithFields() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-fields.json"));
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_fields_foo_field")).isEqualTo("bar");
    }

    @Test
    public void decodeMessagesHandlesMetricbeatMessages() throws Exception {
        final String[] testFiles = {
                "metricbeat-docker-container.json",
                "metricbeat-docker-cpu.json",
                "metricbeat-docker-diskio.json",
                "metricbeat-docker-info.json",
                "metricbeat-docker-memory.json",
                "metricbeat-docker-network.json",
                "metricbeat-mongodb-status.json",
                "metricbeat-mysql-status.json",
                "metricbeat-system-core.json",
                "metricbeat-system-cpu.json",
                "metricbeat-system-filesystem.json",
                "metricbeat-system-fsstat.json",
                "metricbeat-system-load.json",
                "metricbeat-system-memory.json",
                "metricbeat-system-network.json",
                "metricbeat-system-process.json"
        };

        for (String testFile : testFiles) {
            final Message message = codec.decode(messageFromJson(testFile));
            assertThat(message).isNotNull();
            assertThat(message.getSource()).isEqualTo("example.local");
            assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 12, 14, 12, 0, DateTimeZone.UTC));
            assertThat(message.getField("beats_type")).isEqualTo("metricbeat");
        }
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithDocker() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-docker.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_docker_id")).isEqualTo("123");
        assertThat(message.getField("beat_docker_name")).isEqualTo("container-1");
        assertThat(message.getField("beat_docker_labels_docker-kubernetes-pod")).isEqualTo("hello");
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithKubernetes() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-kubernetes.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_kubernetes_pod_name")).isEqualTo("testpod");
        assertThat(message.getField("beat_kubernetes_namespace")).isEqualTo("testns");
        assertThat(message.getField("beat_kubernetes_labels_labelkey")).isEqualTo("labelvalue");
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithCloudAlibaba() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-cloud-alibaba.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_meta_cloud_provider")).isEqualTo("ecs");
        assertThat(message.getField("beat_meta_cloud_instance_id")).isEqualTo("i-wz9g2hqiikg0aliyun2b");
        assertThat(message.getField("beat_meta_cloud_availability_zone")).isEqualTo("cn-shenzhen");
        assertThat(message.getField("beat_meta_cloud_region")).isEqualTo("cn-shenzhen-a");
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithCloudDigitalOcean() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-cloud-digital-ocean.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_meta_cloud_provider")).isEqualTo("digitalocean");
        assertThat(message.getField("beat_meta_cloud_instance_id")).isEqualTo("1234567");
        assertThat(message.getField("beat_meta_cloud_region")).isEqualTo("nyc2");
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithCloudEC2() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-cloud-ec2.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_meta_cloud_provider")).isEqualTo("ec2");
        assertThat(message.getField("beat_meta_cloud_machine_type")).isEqualTo("t2.medium");
        assertThat(message.getField("beat_meta_cloud_instance_id")).isEqualTo("i-4e123456");
        assertThat(message.getField("beat_meta_cloud_region")).isEqualTo("us-east-1");
        assertThat(message.getField("beat_meta_cloud_availability_zone")).isEqualTo("us-east-1c");
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithCloudGCE() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-cloud-gce.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_meta_cloud_provider")).isEqualTo("gce");
        assertThat(message.getField("beat_meta_cloud_machine_type")).isEqualTo("projects/1234567890/machineTypes/f1-micro");
        assertThat(message.getField("beat_meta_cloud_instance_id")).isEqualTo("1234556778987654321");
        assertThat(message.getField("beat_meta_cloud_project_id")).isEqualTo("my-dev");
        assertThat(message.getField("beat_meta_cloud_availability_zone")).isEqualTo("projects/1234567890/zones/us-east1-b");
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithCloudTencent() throws Exception {
        final Message message = codec.decode(messageFromJson("generic-with-cloud-tencent.json"));
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("-");
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
        assertThat(message.getField("beat_meta_cloud_provider")).isEqualTo("qcloud");
        assertThat(message.getField("beat_meta_cloud_instance_id")).isEqualTo("ins-qcloudv5");
        assertThat(message.getField("beat_meta_cloud_region")).isEqualTo("china-south-gz");
        assertThat(message.getField("beat_meta_cloud_availability_zone")).isEqualTo("gz-azone2");
    }

    private RawMessage messageFromJson(String resourceName) throws IOException {
        final URL resource = Resources.getResource(this.getClass(), resourceName);
        final byte[] json = Resources.toByteArray(resource);
        return new RawMessage(json);
    }
}