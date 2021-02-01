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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class GIMMapping extends IndexMapping {
    private static final Set<String> KEYWORD_FIELDS = ImmutableSet.<String>builder()
            .add("gl2_gims_version")
            .add("gl2_tags")
            .add("gl2_event_category")
            .add("gl2_event_subcategory")
            .add("gl2_event_type")
            .add("user_command")
            .add("user_command_path")
            .add("user_domain")
            .add("user_email")
            .add("user_id")
            .add("user_category")
            .add("user_type")
            .add("user_priority")
            .add("source_user_domain")
            .add("source_user_email")
            .add("source_user_id")
            .add("source_user_category")
            .add("source_user_type")
            .add("source_user_priority")
            .add("target_user_domain")
            .add("target_user_email")
            .add("target_user_id")
            .add("target_user_category")
            .add("target_user_type")
            .add("target_user_priority")
            .add("http_application")
            .add("http_content_type")
            .add("http_host")
            .add("http_path")
            .add("http_referrer")
            .add("http_method")
            .add("http_response")
            .add("http_user_agent")
            .add("http_user_agent_name")
            .add("http_url")
            .add("http_url_category")
            .add("http_user_agent_os")
            .add("http_version")
            .add("http_xff")
            .add("host_as_domain")
            .add("host_as_isp")
            .add("host_as_organization")
            .add("host_as_number")
            .add("host_geo_city")
            .add("host_geo_country")
            .add("host_geo_name")
            .add("host_geo_country_iso")
            .add("host_geo_coordinates")
            .add("host_location_name")
            .add("host_mac")
            .add("host_category")
            .add("host_priority")
            .add("host_id")
            .add("host_type")
            .add("host_type_version")
            .add("host_virtfw_id")
            .add("host_virtfw_uid")
            .add("source_as_organization")
            .add("source_as_domain")
            .add("source_as_number")
            .add("source_geo_city")
            .add("source_geo_country")
            .add("source_geo_name")
            .add("source_geo_country_iso")
            .add("source_geo_coordinates")
            .add("source_category")
            .add("source_location_name")
            .add("source_mac")
            .add("source_priority")
            .add("source_id")
            .add("source_type")
            .add("source_vsys_uuid")
            .add("source_zone")
            .add("destination_application_name")
            .add("destination_as_domain")
            .add("destination_as_isp")
            .add("destination_as_organization")
            .add("destination_as_number")
            .add("destination_geo_city_name")
            .add("destination_geo_country_name")
            .add("destination_geo_name")
            .add("destination_geo_state_name")
            .add("destination_geo_country_iso")
            .add("destination_geo_coordinates")
            .add("destination_category")
            .add("destination_location_name")
            .add("destination_mac")
            .add("destination_priority")
            .add("destination_id")
            .add("destination_type")
            .add("destination_vsys_uuid")
            .add("destination_zone")
            .add("event_action")
            .add("event_source")
            .add("event_source_api_version")
            .add("event_source_product")
            .add("event_code")
            .add("event_error_code")
            .add("event_error_description")
            .add("event_log_name")
            .add("event_reporter")
            .add("event_uid")
            .add("event_observer_uid")
            .add("event_outcome")
            .add("event_severity")
            .add("email_message_id")
            .add("email_subject")
            .add("file_company")
            .add("file_name")
            .add("file_path")
            .add("file_size")
            .add("file_type")
            .add("hash_md5")
            .add("hash_sha1")
            .add("hash_sha256")
            .add("hash_sha512")
            .add("hash_imphash")
            .add("alert_definitions_version")
            .add("alert_category")
            .add("alert_indicator")
            .add("alert_signature")
            .add("alert_signature_id")
            .add("alert_severity")
            .add("associated_host")
            .add("associated_mac")
            .add("associated_hash")
            .add("associated_category")
            .add("associated_session_id")
            .add("associated_user_id")
            .add("service_version")
            .add("service_name")
            .add("vendor_alert_severity")
            .add("vendor_event_action")
            .add("vendor_event_description")
            .add("vendor_event_outcome")
            .add("vendor_event_outcome_reason")
            .add("vendor_event_severity")
            .add("vendor_signin_protocol")
            .add("vendor_threat_suspected")
            .add("vendor_transaction_id")
            .add("vendor_transaction_type")
            .add("vendor_user_type")
            .add("windows_logon_type_description")
            .add("windows_kerberos_encryption")
            .add("windows_kerberos_encryption_type")
            .add("windows_kerberos_service_name")
            .add("windows_authentication_package_name")
            .add("windows_authentication_lmpackage_name")
            .add("windows_authentication_process_name")
            .add("source_user_sid_authority1")
            .add("source_user_sid_authority2")
            .add("source_user_sid_rid")
            .add("user_sid_authority1")
            .add("user_sid_authority2")
            .add("user_sid_rid")
            .add("target_sid_authority1")
            .add("target_sid_authority2")
            .add("target_sid_rid")
            .add("network_name")
            .add("network_community_id")
            .add("network_direction")
            .add("network_icmp_type")
            .add("network_ip_version")
            .add("network_transport")
            .add("network_tunnel_type")
            .add("application_sso_signonmode")
            .add("application_sso_target_name")
            .add("threat_category")
            .add("threat_detected")
            .build();

    private static final Set<String> LOWERCASE_KEYWORD_FIELDS = ImmutableSet.<String>builder()
            .add("user_previous_name")
            .add("previous_user_name")
            .add("user_name_mapped")
            .add("source_user_name_mapped")
            .add("target_user_name_mapped")
            .add("host_reference")
            .add("host_hostname")
            .add("host_virtfw_hostname")
            .add("source_reference")
            .add("source_hostname")
            .add("destination_domain")
            .add("destination_reference")
            .add("destination_hostname")
            .add("event_observer_hostname")
            .add("associated_user_name")
            .add("network_application")
            .add("network_interface_in")
            .add("network_interface_out")
            .add("network_protocol")
            .add("application_name")
            .build();

    private static final Set<String> BYTE_FIELDS = ImmutableSet.<String>builder()
            .add("user_priority_level")
            .add("source_user_priority_level")
            .add("target_user_priority_level")
            .add("host_priority_level")
            .add("source_priority_level")
            .add("destination_priority_level")
            .add("event_severity_level")
            .add("alert_severity_level")
            .add("windows_logon_type")
            .build();

    private static final Set<String> LONG_FIELDS = ImmutableSet.<String>builder()
            .add("http_bytes")
            .add("http_request_bytes")
            .add("http_response_bytes")
            .add("http_url_length")
            .add("http_user_agent_length")
            .add("source_bytes_sent")
            .add("source_packets")
            .add("destination_bytes_sent")
            .add("destination_packets_sent")
            .add("event_duration")
            .add("event_repeat_count")
            .add("network_bytes")
            .add("network_bytes_rx")
            .add("network_bytes_tx")
            .add("network_data_bytes")
            .add("network_header_bytes")
            .add("network_packets")
            .add("network_tunnel_duration")
            .build();

    private static final Set<String> INTEGER_FIELDS = ImmutableSet.<String>builder()
            .add("gl2_event_type_code")
            .add("http_response_code")
            .add("source_nat_port")
            .add("destination_nat_port")
            .add("destination_port")
            .add("vendor_alert_severity_level")
            .add("vendor_event_severity_level")
            .add("network_iana_number")
            .build();

    private static final Set<String> IP_FIELDS = ImmutableSet.<String>builder()
            .add("associated_ip")
            .add("vendor_private_ip")
            .add("vendor_private_ipv6")
            .add("vendor_public_ip")
            .add("vendor_public_ipv6")
            .build();

    private static final Set<String> ASSOCIATED_IP_FIELDS = ImmutableSet.<String>builder()
            .add("host_ip")
            .add("source_ip")
            .add("source_nat_ip")
            .add("destination_ip")
            .add("destination_nat_ip")
            .add("network_forwarded_ip")
            .add("event_observer_ip")
            .build();

    private static final Set<String> DATE_FIELDS = ImmutableSet.<String>builder()
            .add("event_created")
            .add("event_start")
            .add("event_received_time")
            .add("file_created_date")
            .build();

    @Override
    protected List<Map<String, Map<String, Object>>> dynamicTemplate() {
        return ImmutableList.<Map<String, Map<String, Object>>>builder()
                .addAll(super.dynamicTemplate())
                .add(Collections.singletonMap(
                        "winlogbeat_fields",
                        ImmutableMap.of(
                                "match", "winlogbeat_*",
                                "match_mapping_type", "string",
                                "mapping", Collections.singletonMap(
                                        "type", "keyword"
                                )
                        )
                ))
                .build();
    }

    private Map<String, Object> notAnalyzedString(Map<String, Object> settings) {
        return ImmutableMap.<String, Object>builder()
                .putAll(notAnalyzedString())
                .putAll(settings)
                .build();
    }

    private Map<String, Object> integerField() {
        return typeField("integer");
    }

    private Map<String, Map<String, Object>> toMapping(Set<String> fields, Supplier<Map<String, Object>> typeFunction) {
        return fields.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        field -> typeFunction.get()
                ));
    }

    private Map<String, Object> notAnalyzedStringWithLowercaseNormalizer() {
        return notAnalyzedString(Collections.singletonMap("normalizer", "loweronly"));
    }

    private Map<String, Object> byteField() {
        return typeField("byte");
    }

    private Map<String, Object> longField() {
        return typeField("long");
    }

    private Map<String, Object> ipField() {
        return typeField("ip");
    }

    private Map<String, Object> dateField() {
        return merge(typeField("date"), Collections.singletonMap(
                "format",
                dateFormats() + "||basic_date_time||basic_date_time_no_millis||epoch_second||date_time_no_millis||date_hour_minute_second_fraction||epoch_millis"
        ));
    }

    protected abstract String dateFormats();

    private Map<String, Object> textField() {
        return typeField("text");
    }

    private Map<String, Object> typeField(String type) {
        return Collections.singletonMap("type", type);
    }

    private Map<String, Object> copyTo(Map<String, Object> source, String newField) {
        return merge(source, Collections.singletonMap("copy_to", newField));
    }

    private Map<String, Object> withAnalyzer(Map<String, Object> source, String analyzer) {
        return merge(source, Collections.singletonMap("analyzer", analyzer));
    }

    private Map<String, Object> merge(Map<String, Object> type1, Map<String, Object> type2) {
        return ImmutableMap.<String, Object>builder()
                .putAll(type1)
                .putAll(type2)
                .build();
    }

    private Map<String, Object> settings() {
        return ImmutableMap.of(
                "index.mapping.ignore_malformed", true,
                "analysis", ImmutableMap.of(
                        "normalizer", ImmutableMap.of(
                                "loweronly", ImmutableMap.of(
                                        "type", "custom",
                                        "char_filter", Collections.emptyList(),
                                        "filter", Collections.singleton("lowercase")
                                )
                        ),
                        "analyzer", analyzerKeyword()
                )
        );
    }

    @Override
    protected Map<String, Map<String, Object>> fieldProperties(String analyzer) {
        return ImmutableMap.<String, Map<String, Object>>builder()
                .putAll(super.fieldProperties(analyzer))
                .putAll(toMapping(KEYWORD_FIELDS, this::notAnalyzedString))
                .putAll(toMapping(LOWERCASE_KEYWORD_FIELDS, this::notAnalyzedStringWithLowercaseNormalizer))
                .putAll(toMapping(BYTE_FIELDS, this::byteField))
                .putAll(toMapping(LONG_FIELDS, this::longField))
                .putAll(toMapping(INTEGER_FIELDS, this::integerField))
                .putAll(toMapping(IP_FIELDS, this::ipField))
                .putAll(toMapping(ASSOCIATED_IP_FIELDS, () -> copyTo(ipField(), "associated_ip")))
                .putAll(toMapping(DATE_FIELDS, this::dateField))
                .put("user_name", copyTo(notAnalyzedStringWithLowercaseNormalizer(), "associated_user_name"))
                .put("source_user_name", copyTo(notAnalyzedStringWithLowercaseNormalizer(), "associated_user_name"))
                .put("target_user_name", copyTo(notAnalyzedStringWithLowercaseNormalizer(), "associated_user_name"))
                .put("source_user_session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put("target_user_session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put("session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put("user_session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put("http_headers", ImmutableMap.<String, Object>builder()
                        .putAll(withAnalyzer(textField(), "standard"))
                        .put("norms", false)
                        .put("index_options", "freqs")
                        .build())
                .put("http_user_agent_analyzed", withAnalyzer(textField(), "standard"))
                .put("http_url_analyzed", withAnalyzer(textField(), "standard"))
                .build();
    }

    @Override
    public Map<String, Object> messageTemplate(String template, String analyzer, int order) {
        return createTemplate(template, order, settings(), mapping(analyzer));
    }
}
