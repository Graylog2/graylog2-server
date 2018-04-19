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
package org.graylog2.inputs.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.graylog2.ConfigurationException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GrokExtractorTest {

    private Set<GrokPattern> patternSet;

    @Before
    public void setUp() throws Exception {
        patternSet = Sets.newHashSet();

        final GrokPattern baseNum = GrokPattern.create("BASE10NUM", "(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))");
        final GrokPattern number = GrokPattern.create("NUMBER", "(?:%{BASE10NUM:UNWANTED})");
        final GrokPattern data = GrokPattern.create("GREEDY", ".*");

        patternSet.add(baseNum);
        patternSet.add(number);
        patternSet.add(data);
    }

    @Test
    public void testDatatypeExtraction() {
        final GrokExtractor extractor = makeExtractor("%{NUMBER:number;int}");

        final Extractor.Result[] results = extractor.run("199999");
        assertEquals("NUMBER is marked as UNWANTED and does not generate a field", 1, results.length);
        assertEquals(Integer.class, results[0].getValue().getClass());
        assertEquals(199999, results[0].getValue());
    }


    @Test
    public void issue_3949() {
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

        final Map<String, Object> config = new HashMap<>();
        config.put("named_captures_only", true);
        assertThatThrownBy(() ->  makeExtractor("%{POSTFIX_SMTPD}", config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No definition for key 'HOSTNAME' found, aborting");
    }

    @Test
    public void testDateExtraction() {
        final GrokExtractor extractor = makeExtractor("%{GREEDY:timestamp;date;yyyy-MM-dd'T'HH:mm:ss.SSSX}");
        final Extractor.Result[] results = extractor.run("2015-07-31T10:05:36.773Z");
        assertEquals("ISO date is parsed", 1, results.length);
        Object value = results[0].getValue();
        assertTrue(value instanceof Instant);
        DateTime date = new DateTime(((Instant) value).toEpochMilli(), DateTimeZone.UTC);

        assertEquals(2015, date.getYear());
        assertEquals(7, date.getMonthOfYear());
        assertEquals(31, date.getDayOfMonth());
        assertEquals(10, date.getHourOfDay());
        assertEquals(5, date.getMinuteOfHour());
        assertEquals(36, date.getSecondOfMinute());
        assertEquals(773, date.getMillisOfSecond());
    }

    @Test
    public void testDateWithComma() {
        final GrokExtractor extractor = makeExtractor("%{GREEDY:timestamp;date;yyyy-MM-dd'T'HH:mm:ss,SSSX}");
        final Extractor.Result[] results = extractor.run("2015-07-31T10:05:36,773Z");
        assertEquals("ISO date is parsed", 1, results.length);
        Object value = results[0].getValue();
        assertTrue(value instanceof Instant);
        DateTime date = new DateTime(((Instant) value).toEpochMilli(), DateTimeZone.UTC);

        assertEquals(2015, date.getYear());
        assertEquals(7, date.getMonthOfYear());
        assertEquals(31, date.getDayOfMonth());
        assertEquals(10, date.getHourOfDay());
        assertEquals(5, date.getMinuteOfHour());
        assertEquals(36, date.getSecondOfMinute());
        assertEquals(773, date.getMillisOfSecond());
    }

    @Test
    public void testNamedCapturesOnly() throws Exception {
        final Map<String, Object> config = new HashMap<>();

        final GrokPattern mynumber = GrokPattern.create("MYNUMBER", "(?:%{BASE10NUM})");

        patternSet.add(mynumber);

        config.put("named_captures_only", true);
        final GrokExtractor extractor1 = makeExtractor("%{MYNUMBER:num}", config);

        config.put("named_captures_only", true);
        final GrokExtractor extractor2 = makeExtractor("%{MYNUMBER:num;int}", config);

        config.put("named_captures_only", false);
        final GrokExtractor extractor3 = makeExtractor("%{MYNUMBER:num}", config);

        final GrokExtractor extractor4 = makeExtractor("%{MYNUMBER:num}");

        assertThat(extractor1.run("2015"))
                .hasSize(1)
                .containsOnly(new Extractor.Result("2015", "num", -1, -1));
        assertThat(extractor2.run("2015"))
                .hasSize(1)
                .containsOnly(new Extractor.Result(2015, "num", -1, -1));
        assertThat(extractor3.run("2015"))
                .hasSize(2)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1),
                        new Extractor.Result("2015", "BASE10NUM", -1, -1)
                );
        assertThat(extractor4.run("2015"))
                .hasSize(2)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1),
                        new Extractor.Result("2015", "BASE10NUM", -1, -1)
                );
    }

    private GrokExtractor makeExtractor(String pattern) {
        return makeExtractor(pattern, new HashMap<>());
    }

    private GrokExtractor makeExtractor(String pattern, Map<String, Object> config) {
        config.put("grok_pattern", pattern);

        try {
            return new GrokExtractor(new LocalMetricRegistry(),
                                     patternSet,
                                     "id",
                                     "title",
                                     0,
                                     Extractor.CursorStrategy.COPY,
                                     "message",
                                     "message",
                                     config,
                                     "admin",
                                     Lists.<Converter>newArrayList(),
                                     Extractor.ConditionType.NONE,
                                     null);
        } catch (Extractor.ReservedFieldException | ConfigurationException e) {
            fail("Test setup is wrong: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}