/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class Beats2CodecTest {

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private Configuration configuration;
    private Beats2Codec codec;
    private final MessageFactory messageFactory = new TestMessageFactory();

    @BeforeEach
    public void setUp() throws Exception {
        configuration = new Configuration(Collections.singletonMap("no_beats_prefix", false));
        codec = new Beats2Codec(configuration, objectMapper, messageFactory);
    }

    @Test
    public void decodeReturnsNullIfPayloadCouldNotBeDecoded() {
        assertThatThrownBy(() -> codec.decodeSafe(new RawMessage(new byte[0])))
                .isInstanceOf(InputProcessingException.class);
    }

    @Test
    public void decodeMessagesHandlesFilebeatMessagesWithoutPrefix() throws Exception {
        configuration = new Configuration(Collections.singletonMap("no_beats_prefix", true));
        codec = new Beats2Codec(configuration, objectMapper, messageFactory);

        final Message message = codec.decodeSafe(messageFromJson("filebeat.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("filebeat.json")).get();
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
    public void decodeMessagesFallsBackToAgentTypeWhenMetadataMissing() throws Exception {
        // Simulates Beat -> Logstash -> Kafka scenario where @metadata is stripped
        final ObjectNode event = objectMapper.createObjectNode();
        event.put("@timestamp", "2025-04-01T19:04:19.678Z");
        event.put("message", "Tue Apr  1 03:04:18 PM EDT 2025");
        event.put("source", "tst-logstash");
        event.put("input_type", "filestream");
        event.put("gl2_source_collector", "4aebb921-aa6f-4352-bfe5-0cdb78a428e6");

        final ObjectNode agent = event.putObject("agent");
        agent.put("type", "filebeat");
        agent.put("name", "tst-logstash");
        agent.put("id", "fb05301d-99f8-45f8-9019-994dbfa6f8a7");

        final ObjectNode host = event.putObject("host");
        host.put("name", "tst-logstash");

        final ObjectNode log = event.putObject("log");
        log.put("offset", 475);
        final ObjectNode logFile = log.putObject("file");
        logFile.put("path", "/home/drew/tmp.txt");

        event.putArray("tags").add("beats_input_codec_plain_applied");

        final Message message = codec.decodeSafe(new RawMessage(objectMapper.writeValueAsBytes(event))).get();
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("Tue Apr  1 03:04:18 PM EDT 2025");
        assertThat(message.getSource()).isEqualTo("tst-logstash");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2025, 4, 1, 19, 4, 19, 678, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("filebeat");
        assertThat(message.getField("filebeat_source")).isEqualTo("tst-logstash");
        assertThat(message.getField("filebeat_agent_type")).isEqualTo("filebeat");
        assertThat(message.getField("filebeat_agent_name")).isEqualTo("tst-logstash");
        assertThat(message.getField("filebeat_agent_id")).isEqualTo("fb05301d-99f8-45f8-9019-994dbfa6f8a7");
        assertThat(message.getField("filebeat_host_name")).isEqualTo("tst-logstash");
        assertThat(message.getField("filebeat_input_type")).isEqualTo("filestream");
        assertThat(message.getField("filebeat_log_offset")).isEqualTo(475);
        assertThat(message.getField("filebeat_log_file_path")).isEqualTo("/home/drew/tmp.txt");
        @SuppressWarnings("unchecked") final List<String> tags = (List<String>) message.getField("filebeat_tags");
        assertThat(tags).containsExactly("beats_input_codec_plain_applied");
        assertThat(message.getField(Message.FIELD_GL2_SOURCE_COLLECTOR)).isEqualTo("4aebb921-aa6f-4352-bfe5-0cdb78a428e6");
        // Verify that beat_ prefix is NOT used
        assertThat(message.getField("beat_agent_type")).isNull();
        assertThat(message.getField("beat_log_file_path")).isNull();
        assertThat(message.getField("beat_tags")).isNull();
    }

    @Test
    public void decodeMessagesFallsBackToBeatTypeForOlderBeats() throws Exception {
        // Simulates older Beats < 7.0 without agent field
        final ObjectNode event = objectMapper.createObjectNode();
        event.put("@timestamp", "2016-04-01T00:00:00.000Z");
        event.put("message", "Test message");

        final ObjectNode beat = event.putObject("beat");
        beat.put("type", "topbeat");
        beat.put("hostname", "example.local");

        event.put("foo", "bar");

        final Message message = codec.decodeSafe(new RawMessage(objectMapper.writeValueAsBytes(event))).get();
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("Test message");
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getField("beats_type")).isEqualTo("topbeat");
        assertThat(message.getField("topbeat_beat_type")).isEqualTo("topbeat");
        assertThat(message.getField("topbeat_foo")).isEqualTo("bar");
    }

    @Test
    public void decodeMessagesDefaultsToBeatWhenNoTypeInformation() throws Exception {
        // Edge case: no metadata, no agent.type, no beat.type
        final ObjectNode event = objectMapper.createObjectNode();
        event.put("@timestamp", "2016-04-01T00:00:00.000Z");
        event.put("message", "Test message");
        event.put("foo", "bar");

        final ObjectNode agent = event.putObject("agent");
        agent.put("hostname", "example.local");
        // No type field

        final Message message = codec.decodeSafe(new RawMessage(objectMapper.writeValueAsBytes(event))).get();
        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("Test message");
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
    }

    @Test
    public void decodeMessagesHandlesPacketbeatMessages() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("packetbeat-dns.json")).get();
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
    public void decodeMessagesHandlesPacketbeatV8Messages() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("packetbeat-mongodb-v8.json")).get();
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2022, 11, 7, 9, 26, 10, 579, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("packetbeat");
        assertThat(message.getField("packetbeat_type")).isEqualTo("mongodb");
        assertThat(message.getField("packetbeat_status")).isEqualTo("OK");
        assertThat(message.getField("packetbeat_method")).isEqualTo("msg");
        assertThat(message.getField("packetbeat_network_bytes")).isEqualTo(557);
        assertThat(message.getField("packetbeat_network_type")).isEqualTo("ipv4");
        assertThat(message.getField("packetbeat_source_ip")).isEqualTo("10.0.55.1");
        assertThat(message.getField("packetbeat_destination_ip")).isEqualTo("10.0.55.2");
        assertThat(message.getField("packetbeat_destination_port")).isEqualTo(27017);
        assertThat(message.getField("packetbeat_host_containerized")).isEqualTo(false);
    }

    @Test
    public void decodeMessagesHandlesTopbeatMessages() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("topbeat-system.json")).get();
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("topbeat");
        assertThat(message.getField("topbeat_type")).isEqualTo("system");
    }

    @Test
    public void decodeMessagesHandlesWinlogbeatMessages() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("winlogbeat.json")).get();
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
    public void decodeMessagesHandlesWinlogbeatv7Messages() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("winlogbeat-v7.json")).get();
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("example.local");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 11, 24, 12, 13, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("winlogbeat");
        assertThat(message.getField("winlogbeat_winlog_level")).isEqualTo("Information");
        assertThat(message.getField("winlogbeat_winlog_event_id")).isEqualTo(5024);
        assertThat(message.getField("winlogbeat_winlog_process_id")).isEqualTo(500);
        assertThat(message.getField("winlogbeat_winlog_log_name")).isEqualTo("Security");
    }

    @Test
    public void decodeMessagesHandleGenericBeatMessages() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("generic.json")).get();
        assertThat(message).isNotNull();
        assertThat(message.getSource()).isEqualTo("unknown");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 4, 1, 0, 0, DateTimeZone.UTC));
        assertThat(message.getField("beats_type")).isEqualTo("beat");
        assertThat(message.getField("beat_foo")).isEqualTo("bar");
    }

    @Test
    public void decodeMessagesHandleGenericBeatMessagesWithFields() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("generic-with-fields.json")).get();
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
            final Message message = codec.decodeSafe(messageFromJson(testFile)).get();
            assertThat(message).isNotNull();
            assertThat(message.getSource()).isEqualTo("example.local");
            assertThat(message.getTimestamp()).isEqualTo(new DateTime(2016, 12, 14, 12, 0, DateTimeZone.UTC));
            assertThat(message.getField("beats_type")).isEqualTo("metricbeat");
        }
    }

    @Test
    public void decodeMessagesHandlesGenericBeatWithDocker() throws Exception {
        final Message message = codec.decodeSafe(messageFromJson("generic-with-docker.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("generic-with-kubernetes.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("generic-with-cloud-alibaba.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("generic-with-cloud-digital-ocean.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("generic-with-cloud-ec2.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("generic-with-cloud-gce.json")).get();
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
        final Message message = codec.decodeSafe(messageFromJson("generic-with-cloud-tencent.json")).get();
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
