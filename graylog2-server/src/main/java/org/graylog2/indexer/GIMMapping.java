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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.schema.AlertFields;
import org.graylog.schema.ApplicationFields;
import org.graylog.schema.AssociatedFields;
import org.graylog.schema.DestinationFields;
import org.graylog.schema.EmailFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.FileFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.HttpFields;
import org.graylog.schema.NetworkFields;
import org.graylog.schema.ServiceFields;
import org.graylog.schema.SessionFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.ThreatFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;

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
            .add(UserFields.USER_COMMAND)
            .add(UserFields.USER_COMMAND_PATH)
            .add(UserFields.USER_DOMAIN)
            .add(UserFields.USER_EMAIL)
            .add(UserFields.USER_ID)
            .add(UserFields.USER_CATEGORY)
            .add(UserFields.USER_TYPE)
            .add(UserFields.USER_PRIORITY)
            .add("source_user_domain")
            .add("source_user_email") // SourceFields.SOURCE_USER_EMAIL is deprecated
            .add("source_user_id")
            .add("source_user_category")
            .add("source_user_type")
            .add("source_user_priority")
            .add("target_user_domain")
            .add(UserFields.TARGET_USER_EMAIL)
            .add(UserFields.TARGET_USER_ID)
            .add("target_user_category")
            .add("target_user_type")
            .add("target_user_priority")
            .add(HttpFields.HTTP_APPLICATION)
            .add(HttpFields.HTTP_CONTENT_TYPE)
            .add(HttpFields.HTTP_HOST)
            .add("http_path")
            .add(HttpFields.HTTP_REFERER)
            .add(HttpFields.HTTP_METHOD)
            .add(HttpFields.HTTP_RESPONSE)
            .add(HttpFields.HTTP_USER_AGENT)
            .add(HttpFields.HTTP_USER_AGENT_NAME)
            .add(HttpFields.HTTP_URL)
            .add(HttpFields.HTTP_URL_CATEGORY)
            .add(HttpFields.HTTP_USER_AGENT_OS)
            .add(HttpFields.HTTP_VERSION)
            .add("http_xff") // HttpFields.HTTP_XFF is "http_version"
            .add(HostFields.HOST_AS_DOMAIN)
            .add(HostFields.HOST_AS_ISP)
            .add("host_as_organization") // There is no HostFields.HOST_AS_ORGANIZATION, only HostFields.HOST_AS_ORGANIZATION_NAME
            .add(HostFields.HOST_AS_NUMBER)
            .add("host_geo_city") // There is no HostFields.HOST_GEO_CITY, only HostFields.HOST_GEO_CITY_NAME
            .add("host_geo_country") // There is no HostFields.HOST_GEO_COUNTRY, only HostFields.HOST_GEO_COUNTRY_NAME
            .add("host_geo_name")
            .add(HostFields.HOST_GEO_COUNTRY_ISO_CODE)
            .add(HostFields.HOST_GEO_COORDINATES)
            .add(HostFields.HOST_LOCATION_NAME)
            .add(HostFields.HOST_MAC)
            .add(HostFields.HOST_CATEGORY)
            .add(HostFields.HOST_PRIORITY)
            .add(HostFields.HOST_ID)
            .add(HostFields.HOST_TYPE)
            .add(HostFields.HOST_TYPE_VERSION)
            .add(HostFields.HOST_VIRTFW_ID)
            .add(HostFields.HOST_VIRTFW_UID)
            .add("source_as_organization")
            .add(SourceFields.SOURCE_AS_DOMAIN)
            .add(SourceFields.SOURCE_AS_NUMBER)
            .add("source_geo_city")
            .add("source_geo_country")
            .add("source_geo_name")
            .add("source_geo_country_iso")
            .add(SourceFields.SOURCE_GEO_COORDINATES)
            .add(SourceFields.SOURCE_CATEGORY)
            .add(SourceFields.SOURCE_LOCATION_NAME)
            .add(SourceFields.SOURCE_MAC)
            .add(SourceFields.SOURCE_PRIORITY)
            .add("source_id")
            .add("source_type")
            .add(SourceFields.SOURCE_VSYS_UUID)
            .add(SourceFields.SOURCE_ZONE)
            .add(DestinationFields.DESTINATION_APPLICATION_NAME)
            .add(DestinationFields.DESTINATION_AS_DOMAIN)
            .add(DestinationFields.DESTINATION_AS_ISP)
            .add("destination_as_organization")
            .add(DestinationFields.DESTINATION_AS_NUMBER)
            .add(DestinationFields.DESTINATION_GEO_CITY_NAME)
            .add(DestinationFields.DESTINATION_GEO_COUNTRY_NAME)
            .add("destination_geo_name")
            .add(DestinationFields.DESTINATION_GEO_STATE_NAME)
            .add("destination_geo_country_iso")
            .add(DestinationFields.DESTINATION_GEO_COORDINATES)
            .add(DestinationFields.DESTINATION_CATEGORY)
            .add(DestinationFields.DESTINATION_LOCATION_NAME)
            .add(DestinationFields.DESTINATION_MAC)
            .add(DestinationFields.DESTINATION_PRIORITY)
            .add("destination_id")
            .add("destination_type")
            .add(DestinationFields.DESTINATION_VSYS_UUID)
            .add(DestinationFields.DESTINATION_ZONE)
            .add(EventFields.EVENT_ACTION)
            .add(EventFields.EVENT_SOURCE)
            .add(EventFields.EVENT_SOURCE_API_VERSION)
            .add(EventFields.EVENT_SOURCE_PRODUCT)
            .add(EventFields.EVENT_CODE)
            .add(EventFields.EVENT_ERROR_CODE)
            .add(EventFields.EVENT_ERROR_DESCRIPTION)
            .add(EventFields.EVENT_LOG_NAME)
            .add(EventFields.EVENT_REPORTER)
            .add(EventFields.EVENT_UID)
            .add(EventFields.EVENT_OBSERVER_UID)
            .add(EventFields.EVENT_OUTCOME)
            .add(EventFields.EVENT_SEVERITY)
            .add(EmailFields.EMAIL_MESSAGE_ID)
            .add(EmailFields.EMAIL_SUBJECT)
            .add(FileFields.FILE_COMPANY)
            .add(FileFields.FILE_NAME)
            .add(FileFields.FILE_PATH)
            .add(FileFields.FILE_SIZE)
            .add(FileFields.FILE_TYPE)
            .add("hash_md5")
            .add("hash_sha1")
            .add("hash_sha256")
            .add("hash_sha512")
            .add("hash_imphash")
            .add(AlertFields.ALERT_DEFINITIONS_VERSION)
            .add(AlertFields.ALERT_CATEGORY)
            .add(AlertFields.ALERT_INDICATOR)
            .add(AlertFields.ALERT_SIGNATURE)
            .add(AlertFields.ALERT_SIGNATURE_ID)
            .add(AlertFields.ALERT_SEVERITY)
            .add(AssociatedFields.ASSOCIATED_HOST)
            .add(AssociatedFields.ASSOCIATED_MAC)
            .add(AssociatedFields.ASSOCIATED_HASH)
            .add(AssociatedFields.ASSOCIATED_CATEGORY)
            .add("associated_session_id")
            .add(AssociatedFields.ASSOCIATED_USER_ID)
            .add(ServiceFields.SERVICE_VERSION)
            .add(ServiceFields.SERVICE_NAME)
            .add(VendorFields.VENDOR_ALERT_SEVERITY)
            .add(VendorFields.VENDOR_EVENT_ACTION)
            .add(VendorFields.VENDOR_EVENT_DESCRIPTION)
            .add(VendorFields.VENDOR_EVENT_OUTCOME)
            .add(VendorFields.VENDOR_EVENT_OUTCOME_REASON)
            .add(VendorFields.VENDOR_EVENT_SEVERITY)
            .add(VendorFields.VENDOR_SIGNIN_PROTOCOL)
            .add(VendorFields.VENDOR_THREAT_SUSPECTED)
            .add(VendorFields.VENDOR_TRANSACTION_ID)
            .add(VendorFields.VENDOR_TRANSACTION_TYPE)
            .add(VendorFields.VENDOR_USER_TYPE)
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
            .add(NetworkFields.NETWORK_NAME)
            .add(NetworkFields.NETWORK_COMMUNITY_ID)
            .add(NetworkFields.NETWORK_DIRECTION)
            .add("network_icmp_type")
            .add(NetworkFields.NETWORK_IP_VERSION)
            .add(NetworkFields.NETWORK_TRANSPORT)
            .add(NetworkFields.NETWORK_TUNNEL_TYPE)
            .add(ApplicationFields.APPLICATION_SSO_SIGNONMODE)
            .add(ApplicationFields.APPLICATION_SSO_TARGET_NAME)
            .add(ThreatFields.THREAT_CATEGORY)
            .add(ThreatFields.THREAT_DETECTED)
            .build();

    private static final Set<String> LOWERCASE_KEYWORD_FIELDS = ImmutableSet.<String>builder()
            .add("user_previous_name")
            .add("previous_user_name")
            .add(UserFields.USER_NAME_MAPPED)
            .add("source_user_name_mapped")
            .add("target_user_name_mapped")
            .add(HostFields.HOST_REFERENCE)
            .add(HostFields.HOST_HOSTNAME)
            .add(HostFields.HOST_VIRTFW_HOSTNAME)
            .add(SourceFields.SOURCE_REFERENCE)
            .add(SourceFields.SOURCE_HOSTNAME)
            .add("destination_domain")
            .add(DestinationFields.DESTINATION_REFERENCE)
            .add(DestinationFields.DESTINATION_HOSTNAME)
            .add(EventFields.EVENT_OBSERVER_HOSTNAME)
            .add(AssociatedFields.ASSOCIATED_USER_NAME)
            .add(NetworkFields.NETWORK_APPLICATION)
            .add(NetworkFields.NETWORK_INTERFACE_IN)
            .add(NetworkFields.NETWORK_INTERFACE_OUT)
            .add(NetworkFields.NETWORK_PROTOCOL)
            .add(ApplicationFields.APPLICATION_NAME)
            .build();

    private static final Set<String> BYTE_FIELDS = ImmutableSet.<String>builder()
            .add(UserFields.USER_PRIORITY_LEVEL)
            .add("source_user_priority_level")
            .add("target_user_priority_level")
            .add(HostFields.HOST_PRIORITY_LEVEL)
            .add(SourceFields.SOURCE_PRIORITY_LEVEL)
            .add(DestinationFields.DESTINATION_PRIORITY_LEVEL)
            .add(EventFields.EVENT_SEVERITY_LEVEL)
            .add(AlertFields.ALERT_SEVERITY_LEVEL)
            .add("windows_logon_type")
            .build();

    private static final Set<String> LONG_FIELDS = ImmutableSet.<String>builder()
            .add(HttpFields.HTTP_BYTES)
            .add(HttpFields.HTTP_REQUEST_BYTES)
            .add(HttpFields.HTTP_RESPONSE_BYTES)
            .add(HttpFields.HTTP_URL_LENGTH)
            .add(HttpFields.HTTP_USER_AGENT_LENGTH)
            .add(SourceFields.SOURCE_BYTES_SENT)
            .add("source_packets")
            .add(DestinationFields.DESTINATION_BYTES_SENT)
            .add(DestinationFields.DESTINATION_PACKETS_SENT)
            .add(EventFields.EVENT_DURATION)
            .add(EventFields.EVENT_REPEAT_COUNT)
            .add(NetworkFields.NETWORK_BYTES)
            .add(NetworkFields.NETWORK_BYTES_RX)
            .add(NetworkFields.NETWORK_BYTES_TX)
            .add(NetworkFields.NETWORK_DATA_BYTES)
            .add(NetworkFields.NETWORK_HEADER_BYTES)
            .add(NetworkFields.NETWORK_PACKETS)
            .add(NetworkFields.NETWORK_TUNNEL_DURATION)
            .build();

    private static final Set<String> INTEGER_FIELDS = ImmutableSet.<String>builder()
            .add("gl2_event_type_code")
            .add(HttpFields.HTTP_RESPONSE_CODE)
            .add(SourceFields.SOURCE_NAT_PORT)
            .add(DestinationFields.DESTINATION_NAT_PORT)
            .add(DestinationFields.DESTINATION_PORT)
            .add(VendorFields.VENDOR_ALERT_SEVERITY_LEVEL)
            .add(VendorFields.VENDOR_EVENT_SEVERITY_LEVEL)
            .add(NetworkFields.NETWORK_IANA_NUMBER)
            .build();

    private static final Set<String> IP_FIELDS = ImmutableSet.<String>builder()
            .add(AssociatedFields.ASSOCIATED_IP)
            .add(VendorFields.VENDOR_PRIVATE_IP)
            .add(VendorFields.VENDOR_PRIVATE_IPV6)
            .add(VendorFields.VENDOR_PUBLIC_IP)
            .add(VendorFields.VENDOR_PUBLIC_IPV6)
            .build();

    private static final Set<String> ASSOCIATED_IP_FIELDS = ImmutableSet.<String>builder()
            .add(HostFields.HOST_IP)
            .add(SourceFields.SOURCE_IP)
            .add(SourceFields.SOURCE_NAT_IP)
            .add(DestinationFields.DESTINATION_IP)
            .add(DestinationFields.DESTINATION_NAT_IP)
            .add(NetworkFields.NETWORK_FORWARDED_IP)
            .add(EventFields.EVENT_OBSERVER_IP)
            .build();

    private static final Set<String> DATE_FIELDS = ImmutableSet.<String>builder()
            .add(EventFields.EVENT_CREATED)
            .add(EventFields.EVENT_START)
            .add(EventFields.EVENT_RECEIVED_TIME)
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
                "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||basic_date_time||basic_date_time_no_millis||epoch_second||date_time_no_millis||date_hour_minute_second_fraction||epoch_millis"
        ));
    }

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
                .putAll(toMapping(ASSOCIATED_IP_FIELDS, () -> copyTo(ipField(), AssociatedFields.ASSOCIATED_IP)))
                .putAll(toMapping(DATE_FIELDS, this::dateField))
                .put(UserFields.USER_NAME, copyTo(notAnalyzedStringWithLowercaseNormalizer(), AssociatedFields.ASSOCIATED_USER_NAME))
                .put("source_user_name", copyTo(notAnalyzedStringWithLowercaseNormalizer(), AssociatedFields.ASSOCIATED_USER_NAME))
                .put(UserFields.TARGET_USER_NAME, copyTo(notAnalyzedStringWithLowercaseNormalizer(), AssociatedFields.ASSOCIATED_USER_NAME))
                .put("source_user_session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put("target_user_session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put(SessionFields.SESSION_ID, copyTo(notAnalyzedString(), "associated_session_id"))
                .put("user_session_id", copyTo(notAnalyzedString(), "associated_session_id"))
                .put(HttpFields.HTTP_HEADERS, ImmutableMap.<String, Object>builder()
                        .putAll(withAnalyzer(textField(), "standard"))
                        .put("norms", false)
                        .put("index_options", "freqs")
                        .build())
                .put(HttpFields.HTTP_USER_AGENT_ANALYZED, withAnalyzer(textField(), "standard"))
                .put(HttpFields.HTTP_URL_ANALYZED, withAnalyzer(textField(), "standard"))
                .build();
    }

    @Override
    public Map<String, Object> messageTemplate(String template, String analyzer, int order) {
        return createTemplate(template, order, settings(), mapping(analyzer));
    }
}
