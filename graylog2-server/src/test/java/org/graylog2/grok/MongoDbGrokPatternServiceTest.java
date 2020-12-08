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
package org.graylog2.grok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MongoDbGrokPatternServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private MongoCollection<Document> collection;
    private MongoDbGrokPatternService service;
    private ClusterEventBus clusterEventBus;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection(MongoDbGrokPatternService.COLLECTION_NAME);
        clusterEventBus = spy(new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor()));

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        service = new MongoDbGrokPatternService(
                mongodb.mongoConnection(),
                mapperProvider,
                clusterEventBus);
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void load() throws NotFoundException {
        final GrokPattern grokPattern = service.load("56250da2d400000000000001");
        assertThat(grokPattern.name()).isEqualTo("Test1");
        assertThat(grokPattern.pattern()).isEqualTo("[a-z]+");
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void loadByNameWithExistingGrokPattern() {
        final Optional<GrokPattern> grokPattern = service.loadByName("Test1");
        assertThat(grokPattern)
                .isPresent()
                .hasValueSatisfying(p -> assertThat(p.name()).isEqualTo("Test1"))
                .hasValueSatisfying(p -> assertThat(p.pattern()).isEqualTo("[a-z]+"));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void loadByNameWithMissingGrokPattern() {
        final Optional<GrokPattern> grokPattern = service.loadByName("DOES_NOT_EXIST");
        assertThat(grokPattern).isEmpty();
    }

    @Test
    public void saveSucceedsWithValidGrokPattern() throws ValidationException {
        service.save(GrokPattern.create("NUMBER", "[0-9]+"));

        verify(clusterEventBus, times(1)).post(any(GrokPatternsUpdatedEvent.class));

        assertThat(collection.count()).isEqualTo(1L);
    }

    @Test
    public void saveFailsWithDuplicateGrokPattern() throws ValidationException {
        service.save(GrokPattern.create("NUMBER", "[0-9]+"));

        assertThatThrownBy(() -> service.save(GrokPattern.create("NUMBER", "[0-9]+")))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("Grok pattern NUMBER already exists");
        assertThat(collection.count()).isEqualTo(1L);

        verify(clusterEventBus, times(1)).post(any(GrokPatternsUpdatedEvent.class));
    }

    @Test
    public void issue_3949() {
        final List<GrokPattern> patternSet = new ArrayList<>();

        // Also see: https://github.com/Graylog2/graylog2-server/issues/3949
        patternSet.add(GrokPattern.create("POSTFIX_QMGR_REMOVED", "%{POSTFIX_QUEUEID:postfix_queueid}: removed"));
        patternSet.add(GrokPattern.create("POSTFIX_CLEANUP_MILTER", "%{POSTFIX_QUEUEID:postfix_queueid}: milter-%{POSTFIX_ACTION:postfix_milter_result}: %{GREEDYDATA:postfix_milter_message}; %{GREEDYDATA_NO_COLON:postfix_keyvalue_data}(: %{GREEDYDATA:postfix_milter_data})?"));
        patternSet.add(GrokPattern.create("POSTFIX_QMGR_ACTIVE", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data} \\(queue active\\)"));
        patternSet.add(GrokPattern.create("POSTFIX_TRIVIAL_REWRITE", "%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_WARNING", "%{POSTFIX_WARNING_WITH_KV}|%{POSTFIX_WARNING_WITHOUT_KV}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD_DISCONNECT", "disconnect from %{POSTFIX_CLIENT_INFO}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD_CONNECT", "connect from %{POSTFIX_CLIENT_INFO}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD_NOQUEUE", "NOQUEUE: %{POSTFIX_ACTION:postfix_action}: %{POSTFIX_SMTP_STAGE:postfix_smtp_stage} from %{POSTFIX_CLIENT_INFO}:( %{POSTFIX_STATUS_CODE:postfix_status_code} %{POSTFIX_STATUS_CODE_ENHANCED:postfix_status_code_enhanced})?( <%{DATA:postfix_status_data}>:)? (%{POSTFIX_DNSBL_MESSAGE}|%{GREEDYDATA:postfix_status_message};) %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD_LOSTCONN", "%{POSTFIX_LOSTCONN:postfix_smtpd_lostconn_data}( after %{POSTFIX_SMTP_STAGE:postfix_smtp_stage}( \\(%{INT} bytes\\))?)? from %{POSTFIX_CLIENT_INFO}(: %{GREEDYDATA:postfix_smtpd_lostconn_reason})?"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD_PROXY", "proxy-%{POSTFIX_ACTION:postfix_proxy_result}: (%{POSTFIX_SMTP_STAGE:postfix_proxy_smtp_stage}): %{POSTFIX_PROXY_MESSAGE:postfix_proxy_message}; %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD_PIPELINING", "improper command pipelining after %{POSTFIX_SMTP_STAGE:postfix_smtp_stage} from %{POSTFIX_CLIENT_INFO}: %{GREEDYDATA:postfix_improper_pipelining_data}"));
        patternSet.add(GrokPattern.create("POSTFIX_QMGR", "%{POSTFIX_QMGR_REMOVED}|%{POSTFIX_QMGR_ACTIVE}|%{POSTFIX_QMGR_EXPIRED}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_CLEANUP", "%{POSTFIX_CLEANUP_MILTER}|%{POSTFIX_WARNING}|%{POSTFIX_KEYVALUE}"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTSCREEN", "%{POSTFIX_PS_CONNECT}|%{POSTFIX_PS_ACCESS}|%{POSTFIX_PS_NOQUEUE}|%{POSTFIX_PS_TOOBUSY}|%{POSTFIX_PS_CACHE}|%{POSTFIX_PS_DNSBL}|%{POSTFIX_PS_VIOLATIONS}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_PIPE", "%{POSTFIX_PIPE_ANY}"));
        patternSet.add(GrokPattern.create("POSTFIX_ANVIL", "%{POSTFIX_ANVIL_CONN_RATE}|%{POSTFIX_ANVIL_CONN_CACHE}|%{POSTFIX_ANVIL_CONN_COUNT}"));
        patternSet.add(GrokPattern.create("POSTFIX_DNSBLOG", "%{POSTFIX_DNSBLOG_LISTING}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_PICKUP", "%{POSTFIX_KEYVALUE}"));
        patternSet.add(GrokPattern.create("POSTFIX_LMTP", "%{POSTFIX_SMTP}"));
        patternSet.add(GrokPattern.create("POSTFIX_MASTER", "%{POSTFIX_MASTER_START}|%{POSTFIX_MASTER_EXIT}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_TLSPROXY", "%{POSTFIX_TLSPROXY_CONN}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_SENDMAIL", "%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_BOUNCE", "%{POSTFIX_BOUNCE_NOTIFICATION}"));
        patternSet.add(GrokPattern.create("POSTFIX_SCACHE", "%{POSTFIX_SCACHE_LOOKUPS}|%{POSTFIX_SCACHE_SIMULTANEOUS}|%{POSTFIX_SCACHE_TIMESTAMP}"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTDROP", "%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_LOSTCONN_REASONS", "(receiving the initial server greeting|sending message body|sending end of data -- message may be sent more than once)"));
        patternSet.add(GrokPattern.create("POSTFIX_PROXY_MESSAGE", "(%{POSTFIX_STATUS_CODE:postfix_proxy_status_code} )?(%{POSTFIX_STATUS_CODE_ENHANCED:postfix_proxy_status_code_enhanced})?.*"));
        patternSet.add(GrokPattern.create("GREEDYDATA_NO_COLON", "[^:]*"));
        patternSet.add(GrokPattern.create("GREEDYDATA_NO_SEMICOLON", "[^;]*"));
        patternSet.add(GrokPattern.create("POSTFIX_DISCARD", "%{POSTFIX_DISCARD_ANY}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_WARNING_WITH_KV", "(%{POSTFIX_QUEUEID:postfix_queueid}: )?%{POSTFIX_WARNING_LEVEL:postfix_message_level}: %{GREEDYDATA:postfix_message}; %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP", "%{POSTFIX_SMTP_DELIVERY}|%{POSTFIX_SMTP_CONNERR}|%{POSTFIX_SMTP_LOSTCONN}|%{POSTFIX_SMTP_TIMEOUT}|%{POSTFIX_SMTP_RELAYERR}|%{POSTFIX_TLSCONN}|%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_WARNING_WITHOUT_KV", "(%{POSTFIX_QUEUEID:postfix_queueid}: )?%{POSTFIX_WARNING_LEVEL:postfix_message_level}: %{GREEDYDATA:postfix_message}"));
        patternSet.add(GrokPattern.create("POSTFIX_TLSPROXY_CONN", "(DIS)?CONNECT( from)? %{POSTFIX_CLIENT_INFO}"));
        patternSet.add(GrokPattern.create("POSTFIX_ANVIL_CONN_CACHE", "statistics: max cache size %{NUMBER:postfix_anvil_cache_size} at %{SYSLOGTIMESTAMP:postfix_anvil_timestamp}"));
        patternSet.add(GrokPattern.create("POSTFIX_ANVIL_CONN_RATE", "statistics: max connection rate %{NUMBER:postfix_anvil_conn_rate}/%{POSTFIX_TIME_UNIT:postfix_anvil_conn_period} for \\(%{DATA:postfix_service}:%{IP:postfix_client_ip}\\) at %{SYSLOGTIMESTAMP:postfix_anvil_timestamp}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP_DELIVERY", "%{POSTFIX_KEYVALUE} status=%{WORD:postfix_status}( \\(%{GREEDYDATA:postfix_smtp_response}\\))?"));
        patternSet.add(GrokPattern.create("POSTFIX_ANVIL_CONN_COUNT", "statistics: max connection count %{NUMBER:postfix_anvil_conn_count} for \\(%{DATA:postfix_service}:%{IP:postfix_client_ip}\\) at %{SYSLOGTIMESTAMP:postfix_anvil_timestamp}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP_CONNERR", "connect to %{POSTFIX_RELAY_INFO}: (Connection timed out|No route to host|Connection refused|Network is unreachable)"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTPD", "%{POSTFIX_SMTPD_CONNECT}|%{POSTFIX_SMTPD_DISCONNECT}|%{POSTFIX_SMTPD_LOSTCONN}|%{POSTFIX_SMTPD_NOQUEUE}|%{POSTFIX_SMTPD_PIPELINING}|%{POSTFIX_TLSCONN}|%{POSTFIX_WARNING}|%{POSTFIX_SMTPD_PROXY}|%{POSTFIX_KEYVALUE}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP_RELAYERR", "%{POSTFIX_QUEUEID:postfix_queueid}: host %{POSTFIX_RELAY_INFO} said: %{GREEDYDATA:postfix_smtp_response} \\(in reply to %{POSTFIX_SMTP_STAGE:postfix_smtp_stage} command\\)"));
        patternSet.add(GrokPattern.create("POSTFIX_STATUS_CODE_ENHANCED", "\\d\\.\\d\\.\\d"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP_TIMEOUT", "%{POSTFIX_QUEUEID:postfix_queueid}: conversation with %{POSTFIX_RELAY_INFO} timed out( while %{POSTFIX_LOSTCONN_REASONS:postfix_smtp_lostconn_reason})?"));
        patternSet.add(GrokPattern.create("POSTFIX_MASTER_EXIT", "terminating on signal %{INT:postfix_termination_signal}"));
        patternSet.add(GrokPattern.create("POSTFIX_MASTER_START", "(daemon started|reload) -- version %{DATA:postfix_version}, configuration %{PATH:postfix_config_path}"));
        patternSet.add(GrokPattern.create("POSTFIX_SCACHE_LOOKUPS", "statistics: (address|domain) lookup hits=%{INT:postfix_scache_hits} miss=%{INT:postfix_scache_miss} success=%{INT:postfix_scache_success}%"));
        patternSet.add(GrokPattern.create("POSTFIX_BOUNCE_NOTIFICATION", "%{POSTFIX_QUEUEID:postfix_queueid}: sender (non-delivery|delivery status|delay) notification: %{POSTFIX_QUEUEID:postfix_bounce_queueid}"));
        patternSet.add(GrokPattern.create("POSTFIX_SCACHE_TIMESTAMP", "statistics: start interval %{SYSLOGTIMESTAMP:postfix_scache_timestamp}"));
        patternSet.add(GrokPattern.create("POSTFIX_SCACHE_SIMULTANEOUS", "statistics: max simultaneous domains=%{INT:postfix_scache_domains} addresses=%{INT:postfix_scache_addresses} connection=%{INT:postfix_scache_connection}"));
        patternSet.add(GrokPattern.create("POSTFIX_CLIENT_INFO", "%{HOSTNAME:postfix_client_hostname}?\\[%{IP:postfix_client_ip}\\](:%{INT:postfix_client_port})?"));
        patternSet.add(GrokPattern.create("POSTFIX_RELAY_INFO", "%{HOSTNAME:postfix_relay_hostname}?\\[(%{IP:postfix_relay_ip}|%{DATA:postfix_relay_service})\\](:%{INT:postfix_relay_port})?|%{WORD:postfix_relay_service}"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP_STAGE", "(CONNECT|HELO|EHLO|STARTTLS|AUTH|MAIL( FROM)?|RCPT( TO)?|(end of )?DATA|RSET|UNKNOWN|END-OF-MESSAGE|VRFY|\\.)"));
        patternSet.add(GrokPattern.create("POSTFIX_ACTION", "(accept|defer|discard|filter|header-redirect|reject)"));
        patternSet.add(GrokPattern.create("POSTFIX_SMTP_LOSTCONN", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_LOSTCONN:postfix_smtp_lostconn_data} with %{POSTFIX_RELAY_INFO}( while %{POSTFIX_LOSTCONN_REASONS:postfix_smtp_lostconn_reason})?"));
        patternSet.add(GrokPattern.create("POSTFIX_STATUS_CODE", "\\d{3}"));
        patternSet.add(GrokPattern.create("POSTFIX_TLSCONN", "(Anonymous|Trusted|Untrusted|Verified) TLS connection established (to %{POSTFIX_RELAY_INFO}|from %{POSTFIX_CLIENT_INFO}): %{DATA:postfix_tls_version} with cipher %{DATA:postfix_tls_cipher} \\(%{DATA:postfix_tls_cipher_size} bits\\)"));
        patternSet.add(GrokPattern.create("POSTFIX_DELAYS", "%{NUMBER:postfix_delay_before_qmgr}/%{NUMBER:postfix_delay_in_qmgr}/%{NUMBER:postfix_delay_conn_setup}/%{NUMBER:postfix_delay_transmission}"));
        patternSet.add(GrokPattern.create("POSTFIX_LOSTCONN", "(lost connection|timeout|SSL_accept error)"));
        patternSet.add(GrokPattern.create("POSTFIX_PIPE_ANY", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data}, status=%{WORD:postfix_status} \\(%{GREEDYDATA:postfix_pipe_response}\\)"));
        patternSet.add(GrokPattern.create("POSTFIX_QMGR_EXPIRED", "%{POSTFIX_QUEUEID:postfix_queueid}: from=<%{DATA:postfix_from}>, status=%{WORD:postfix_status}, returned to sender"));
        patternSet.add(GrokPattern.create("POSTFIX_DISCARD_ANY", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data} status=%{WORD:postfix_status} %{GREEDYDATA}"));
        patternSet.add(GrokPattern.create("POSTFIX_ERROR_ANY", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data}, status=%{WORD:postfix_status} \\(%{GREEDYDATA:postfix_error_response}\\)"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTSUPER_ACTION", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_POSTSUPER_ACTIONS:postfix_postsuper_action}"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTSUPER_ACTIONS", "(removed|requeued|placed on hold|released from hold)"));
        patternSet.add(GrokPattern.create("POSTFIX_DNSBLOG_LISTING", "addr %{IP:postfix_client_ip} listed by domain %{HOSTNAME:postfix_dnsbl_domain} as %{IP:postfix_dnsbl_result}"));
        patternSet.add(GrokPattern.create("POSTFIX_DNSBL_MESSAGE", "Service unavailable; .* \\[%{GREEDYDATA:postfix_status_data}\\] %{GREEDYDATA:postfix_status_message};"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_VIOLATIONS", "%{POSTFIX_PS_VIOLATION:postfix_postscreen_violation}( %{INT})?( after %{NUMBER:postfix_postscreen_violation_time})? from %{POSTFIX_CLIENT_INFO}(( after %{POSTFIX_SMTP_STAGE:postfix_smtp_stage})?(: %{GREEDYDATA:postfix_postscreen_data})?| in tests (after|before) SMTP handshake)"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_ACCESS_ACTION", "(DISCONNECT|BLACKLISTED|WHITELISTED|WHITELIST VETO|PASS NEW|PASS OLD)"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_VIOLATION", "(BARE NEWLINE|COMMAND (TIME|COUNT|LENGTH) LIMIT|COMMAND PIPELINING|DNSBL|HANGUP|NON-SMTP COMMAND|PREGREET)"));
        patternSet.add(GrokPattern.create("POSTFIX_TIME_UNIT", "%{NUMBER}[smhd]"));
        patternSet.add(GrokPattern.create("POSTFIX_KEYVALUE_DATA", "[\\w-]+=[^;]*"));
        patternSet.add(GrokPattern.create("POSTFIX_KEYVALUE", "%{POSTFIX_QUEUEID:postfix_queueid}: %{POSTFIX_KEYVALUE_DATA:postfix_keyvalue_data}"));
        patternSet.add(GrokPattern.create("POSTFIX_WARNING_LEVEL", "(warning|fatal|info)"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTSUPER_SUMMARY", "%{POSTFIX_POSTSUPER_SUMMARY_ACTIONS:postfix_postsuper_summary_action}: %{NUMBER:postfix_postsuper_summary_count} messages?"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTSUPER_SUMMARY_ACTIONS", "(Deleted|Requeued|Placed on hold|Released from hold)"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_ACCESS", "%{POSTFIX_PS_ACCESS_ACTION:postfix_postscreen_access} %{POSTFIX_CLIENT_INFO}"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_CONNECT", "CONNECT from %{POSTFIX_CLIENT_INFO} to \\[%{IP:postfix_server_ip}\\]:%{INT:postfix_server_port}"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_TOOBUSY", "NOQUEUE: reject: CONNECT from %{POSTFIX_CLIENT_INFO}: %{GREEDYDATA:postfix_postscreen_toobusy_data}"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_NOQUEUE", "%{POSTFIX_SMTPD_NOQUEUE}"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_CACHE", "cache %{DATA} full cleanup: retained=%{NUMBER:postfix_postscreen_cache_retained} dropped=%{NUMBER:postfix_postscreen_cache_dropped} entries"));
        patternSet.add(GrokPattern.create("POSTFIX_PS_DNSBL", "%{POSTFIX_PS_VIOLATION:postfix_postscreen_violation} rank %{INT:postfix_postscreen_dnsbl_rank} for %{POSTFIX_CLIENT_INFO}"));
        patternSet.add(GrokPattern.create("POSTFIX_LOCAL", "%{POSTFIX_KEYVALUE}"));
        patternSet.add(GrokPattern.create("POSTFIX_TLSMGR", "%{POSTFIX_WARNING}"));
        patternSet.add(GrokPattern.create("POSTFIX_ERROR", "%{POSTFIX_ERROR_ANY}"));
        patternSet.add(GrokPattern.create("POSTFIX_QUEUEID", "([0-9A-F]{6,}|[0-9a-zA-Z]{15,})"));
        patternSet.add(GrokPattern.create("POSTFIX_VIRTUAL", "%{POSTFIX_SMTP_DELIVERY}"));
        patternSet.add(GrokPattern.create("POSTFIX_POSTSUPER", "%{POSTFIX_POSTSUPER_ACTION}|%{POSTFIX_POSTSUPER_SUMMARY}"));

        assertThatThrownBy(() ->  service.saveAll(patternSet, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No definition for key 'GREEDYDATA' found, aborting");
    }

    @Test
    public void saveAllSucceedsWithValidGrokPatterns() throws ValidationException {
        final List<GrokPattern> grokPatterns = Arrays.asList(
                GrokPattern.create("NUMBER", "[0-9]+"),
                GrokPattern.create("INT", "[+-]?%{NUMBER}"));
        service.saveAll(grokPatterns, false);

        verify(clusterEventBus, times(1)).post(any(GrokPatternsUpdatedEvent.class));

        assertThat(collection.count()).isEqualTo(2L);
    }

    @Test
    public void saveAllSucceedsWithDuplicateGrokPatternWithReplaceAll() throws ValidationException {
        final List<GrokPattern> grokPatterns = Arrays.asList(
                GrokPattern.create("NUMBER", "[0-9]+"),
                GrokPattern.create("INT", "[+-]?%{NUMBER}"));
        service.save(GrokPattern.create("NUMBER", "[0-9]+"));

        service.saveAll(grokPatterns, true);

        assertThat(collection.count()).isEqualTo(2L);
        verify(clusterEventBus, times(2)).post(any(GrokPatternsUpdatedEvent.class));
    }

    @Test
    public void loadNonExistentGrokPatternThrowsNotFoundException() {
        assertThatThrownBy(() -> service.load("cafebabe00000000deadbeef"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void bulkLoad() {
        final List<String> idList = ImmutableList.of(
                "56250da2d400000000000001",
                "56250da2d400000000000002",
                "56250da2d4000000deadbeef");

        final Set<GrokPattern> grokPatterns = service.bulkLoad(idList);
        assertThat(grokPatterns)
                .hasSize(2)
                .contains(
                        GrokPattern.create("56250da2d400000000000001", "Test1", "[a-z]+", null),
                        GrokPattern.create("56250da2d400000000000002", "Test2", "[a-z]+", null));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void bulkLoadReturnsEmptySetIfGrokPatternsNotFound() {
        final List<String> idList = ImmutableList.of("56250da2d4000000deadbeef");

        final Set<GrokPattern> grokPatterns = service.bulkLoad(idList);
        assertThat(grokPatterns).isEmpty();
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void loadAll() {
        final Set<GrokPattern> grokPatterns = service.loadAll();
        assertThat(grokPatterns)
                .hasSize(3)
                .contains(
                        GrokPattern.create("56250da2d400000000000001", "Test1", "[a-z]+", null),
                        GrokPattern.create("56250da2d400000000000002", "Test2", "[a-z]+", null),
                        GrokPattern.create("56250da2d400000000000003", "Test3", "%{Test1}-%{Test2}", "56250da2deadbeefcafebabe"));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void deleteAll() {
        assertThat(collection.count()).isEqualTo(3);

        final int deletedRecords = service.deleteAll();
        assertThat(deletedRecords).isEqualTo(3);
        assertThat(collection.count()).isEqualTo(0);
        verify(clusterEventBus, times(1)).post(any(GrokPatternsDeletedEvent.class));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void delete() {
        assertThat(collection.count()).isEqualTo(3);

        final int deletedRecords = service.delete("56250da2d400000000000001");
        assertThat(deletedRecords).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(2);
        verify(clusterEventBus, times(1)).post(any(GrokPatternsDeletedEvent.class));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void deleteNonExistentGrokPattern() {
        assertThat(collection.count()).isEqualTo(3);

        final int deletedRecords = service.delete("56250da2d4000000deadbeef");
        assertThat(deletedRecords).isEqualTo(0);
        assertThat(collection.count()).isEqualTo(3);
        verify(clusterEventBus, never()).post(any(GrokPatternsDeletedEvent.class));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void saveAllWithoutReplace() throws ValidationException {
        assertThat(collection.count()).isEqualTo(3);

        final List<GrokPattern> grokPatterns = ImmutableList.of(
                GrokPattern.create("Test", "Pattern"),
                GrokPattern.create("56250da2d400000000000001", "Test", "Pattern", null)
        );
        final List<GrokPattern> savedGrokPatterns = service.saveAll(grokPatterns, false);
        assertThat(savedGrokPatterns).hasSize(2);
        assertThat(collection.count()).isEqualTo(4);
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void saveAllWithReplace() throws ValidationException {
        assertThat(collection.count()).isEqualTo(3);

        final List<GrokPattern> grokPatterns = ImmutableList.of(GrokPattern.create("Test", "Pattern"));
        final List<GrokPattern> savedGrokPatterns = service.saveAll(grokPatterns, true);
        assertThat(savedGrokPatterns).hasSize(1);
        assertThat(collection.count()).isEqualTo(1);
    }

    @Test
    public void saveAllFailsWithDuplicateGrokPatternWithoutReplaceAll() throws ValidationException {
        final List<GrokPattern> grokPatterns = Arrays.asList(
                GrokPattern.create("NUMBER", "[0-9]+"),
                GrokPattern.create("INT", "[+-]?%{NUMBER}"));
        service.save(GrokPattern.create("NUMBER", "[0-9]+"));

        assertThatThrownBy(() -> service.saveAll(grokPatterns, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("Grok pattern NUMBER already exists");
        assertThat(collection.count()).isEqualTo(1L);

        verify(clusterEventBus, times(1)).post(any(GrokPatternsUpdatedEvent.class));
    }

    @Test
    public void saveAllWithInvalidGrokPattern() {
        final List<GrokPattern> grokPatterns = ImmutableList.of(
                GrokPattern.create("Test", "Pattern"),
                GrokPattern.create("Test", "")
        );
        assertThatThrownBy(() -> service.saveAll(grokPatterns, true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    public void saveValidGrokPattern() throws ValidationException, NotFoundException {
        assertThat(collection.count()).isEqualTo(0);

        final GrokPattern savedGrokPattern = service.save(GrokPattern.create("Test", "Pattern"));
        assertThat(collection.count()).isEqualTo(1);
        final GrokPattern grokPattern = service.load(savedGrokPattern.id());
        assertThat(grokPattern).isEqualTo(savedGrokPattern);

        verify(clusterEventBus, times(1)).post(any(GrokPatternsUpdatedEvent.class));
    }

    @Test
    public void saveInvalidGrokPattern() {
        assertThat(collection.count()).isEqualTo(0);

        assertThatThrownBy(() -> service.save(GrokPattern.create("Test", "%{")))
                .isInstanceOf(ValidationException.class);
        assertThat(collection.count()).isEqualTo(0);

        assertThatThrownBy(() -> service.save(GrokPattern.create("", "[a-z]+")))
                .isInstanceOf(ValidationException.class);
        assertThat(collection.count()).isEqualTo(0);

        assertThatThrownBy(() -> service.save(GrokPattern.create("Test", "")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(collection.count()).isEqualTo(0);

        verify(clusterEventBus, never()).post(any(GrokPatternsUpdatedEvent.class));
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void validateValidGrokPattern() {
        assertThat(service.validate(GrokPattern.create("Test", "%{Test1}"))).isTrue();
    }

    @Test
    @Ignore("Disabled until MongoDbGrokPatternService#validate() has been fixed")
    public void validateInvalidGrokPattern() {
        assertThat(service.validate(GrokPattern.create("Test", "%{"))).isFalse();
        assertThat(service.validate(GrokPattern.create("Test", ""))).isFalse();
        assertThat(service.validate(GrokPattern.create("", "[a-z]+"))).isFalse();
    }

    @Test
    @MongoDBFixtures("MongoDbGrokPatternServiceTest.json")
    public void update() throws ValidationException {
        assertThat(collection.count()).isEqualTo(3);

        GrokPattern toUpdate1 = GrokPattern.builder()
                .id("56250da2d400000000000001")
                .name("Test1")
                .pattern("123")
                .build();
        final GrokPattern updatedPattern1 = service.update(toUpdate1);
        assertThat(updatedPattern1.name()).matches(toUpdate1.name());
        assertThat(updatedPattern1.pattern()).matches(toUpdate1.pattern());
        assertThat(collection.count()).isEqualTo(3);

        GrokPattern toUpdate2 = GrokPattern.builder()
                .id("56250da2d400000000000001")
                .name("Testxxx")
                .pattern("123")
                .build();
        final GrokPattern updatedPattern2 = service.update(toUpdate2);
        assertThat(updatedPattern2.name()).matches(toUpdate2.name());
        assertThat(updatedPattern2.pattern()).matches(toUpdate2.pattern());
        assertThat(collection.count()).isEqualTo(3);

        GrokPattern toUpdate3 = GrokPattern.builder()
                .name("Testxxx")
                .pattern("123")
                .build();
        boolean thrown = false;
        try {
            service.update(toUpdate3);
        } catch (ValidationException e) {
            thrown = true;
        }
        assertThat(thrown).isTrue();
        assertThat(collection.count()).isEqualTo(3);

        GrokPattern toUpdate4 = GrokPattern.builder()
                .id("56250da2d400000000000321")
                .name("Testxxx")
                .pattern("123")
                .build();
        thrown = false;
        try {
            service.update(toUpdate4);
        } catch (ValidationException e) {
            thrown = true;
        }
        assertThat(thrown).isTrue();
        assertThat(collection.count()).isEqualTo(3);

        verify(clusterEventBus, times(2)).post(any(GrokPatternsUpdatedEvent.class));
    }
}
