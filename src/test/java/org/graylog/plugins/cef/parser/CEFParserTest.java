package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CEFParserTest {
    private CEFParser parser;
    
    @Before
    public void setUp() {
        parser = new CEFParser(false);
    }

    @Test
    public void testParseExtensionsUseCustomLabelsWithFullNames() throws Exception {
        final CEFParser parser = new CEFParser(true);
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 c6a1=fe80::5626:96ff:fed0:943 c6a1Label=TestTest foobar=quux");
        assertEquals("ip-172-30-2-212", r.get("deviceAddress"));
        assertEquals("fe80::5626:96ff:fed0:943", r.get("TestTest"));
        assertEquals("quux", r.get("foobar"));
    }

    @Test
    public void testParseExtensionsCustomIPv6() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 c6a1=fe80::5626:96ff:fed0:943 c6a1Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals("fe80::5626:96ff:fed0:943", r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomFloat() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 cfp2=90.01 cfp2Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(90.01F, r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomLong() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 cn1=999999999999 cn1Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(999999999999L, r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomLongFlex() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 flexNumber2=999999999999 flexNumber2Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(999999999999L, r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomString() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 cs5=Fooo! cs5Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals("Fooo!", r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomStringFlex() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 flexString1=Fooo! flexString1Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals("Fooo!", r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomTimestamp() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 deviceCustomDate1=2016-08-19T21:51:08+00:00 deviceCustomDate1Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(DateTime.parse("2016-08-19T21:51:08+00:00"), r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomTimestampFlex() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 flexDate1=2016-08-19T21:51:08+00:00 flexDate1Label=TestTest cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(DateTime.parse("2016-08-19T21:51:08+00:00"), r.get("TestTest"));
    }

    @Test
    public void testParseExtensionsCustomMixed() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 cn1=999999999999 cn1Label=TestTest cfp2=90.01 cfp2Label=TestTest2 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(999999999999L, r.get("TestTest"));
        assertEquals(90.01F, r.get("TestTest2"));
    }

    @Test
    public void testParseExtensionsStandardString() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals("ip-172-30-2-212", r.get("dvc"));
    }

    @Test
    public void testParseExtensionsStandardInteger() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 cnt=3 destinationTranslatedPort=22 deviceDirection=1 dpid=9001 dpt=3342 dvcpid=900 fsize=12 in=543 oldFileSize=1000 sourceTranslatedPort=443 spid=5516 spt=22 type=0 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(3, r.get("cnt"));
        assertEquals(22, r.get("destinationTranslatedPort"));
        assertEquals(CEFMapping.Direction.OUTBOUND, r.get("deviceDirection"));
        assertEquals(9001, r.get("dpid"));
        assertEquals(3342, r.get("dpt"));
        assertEquals(900, r.get("dvcpid"));
        assertEquals(12, r.get("fsize"));
        assertEquals(543, r.get("in"));
        assertEquals(1000, r.get("oldFileSize"));
        assertEquals(443, r.get("sourceTranslatedPort"));
        assertEquals(5516, r.get("spid"));
        assertEquals(22, r.get("spt"));
        assertEquals(CEFMapping.Type.BASE_EVENT, r.get("type"));
    }

    @Test
    public void testParseExtensionsStandardDouble() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 slat=29.7604 slong=95.3698 dlat=53.5511 dlong=9.9937 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(29.7604D, r.get("slat"));
        assertEquals(95.3698, r.get("slong"));

        assertEquals(53.5511, r.get("dlat"));
        assertEquals(9.9937, r.get("dlong"));
    }

    @Test
    public void testParseExtensionsStandardLong() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 eventId=9001 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals(9001L, r.get("eventId"));
    }

    @Test
    public void testParseExtensionsSpacedValues() throws Exception {
        Map<String, Object> r = parser.parseExtensions("dvc=ip-172-30-2-212 eventId=9001 cs2=ip-172-30-2-212 -> /var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: message repeated 2 times");
        assertEquals("ip-172-30-2-212 -> /var/log/auth.log", r.get("Location"));
    }

    @Test
    public void testParseExtensionsEscapedMultiLine() throws Exception {
        Map<String, Object> r = parser.parseExtensions("custom1=new\\rline custom2=new\\nline custom3=new\\r\\nline msg=Foobar");
        assertEquals("new\rline", r.get("custom1"));
        assertEquals("new\nline", r.get("custom2"));
        assertEquals("new\r\nline", r.get("custom3"));
    }

    @Test
    public void testParseExtensionsEscapedEqualSign() throws Exception {
        Map<String, Object> r = parser.parseExtensions("custom=equal\\=sign msg=Foobar");
        assertEquals("equal=sign", r.get("custom"));
    }

    @Test
    public void testParseExtensionsEscapedBackslash() throws Exception {
        Map<String, Object> r = parser.parseExtensions("custom=back\\\\slash msg=Foobar");
        assertEquals("back\\slash", r.get("custom"));
    }

    @Test
    public void testParse() throws Exception {
        CEFMessage m = parser.parse("CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cfp2=90.01 cfp2Label=SomeFloat spt=22 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: PAM 2 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=116.31.116.17  user=root").build();

        assertEquals(0, m.version());
        assertEquals("Trend Micro Inc.", m.deviceVendor());
        assertEquals("OSSEC HIDS", m.deviceProduct());
        assertEquals("v2.8.3", m.deviceVersion());
        assertEquals("2502", m.deviceEventClassId());
        assertEquals("User missed the password more than one time", m.name());
        assertEquals(10, m.severity().numeric());
        assertEquals("Very-High", m.severity().text());

        assertEquals("Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: PAM 2 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=116.31.116.17  user=root", m.message());

        assertEquals("ip-172-30-2-212", m.fields().get("dvc"));
        assertEquals(22, m.fields().get("spt"));
        assertEquals(90.01F, m.fields().get("SomeFloat"));
        assertEquals("ip-172-30-2-212->/var/log/auth.log", m.fields().get("Location"));
    }

    @Test
    public void testParseWithMissingMsgField() throws Exception {
        CEFMessage m = parser.parse("CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location cfp2=90.01 cfp2Label=SomeFloat spt=22 ").build();

        assertEquals(0, m.version());
        assertEquals("Trend Micro Inc.", m.deviceVendor());
        assertEquals("OSSEC HIDS", m.deviceProduct());
        assertEquals("v2.8.3", m.deviceVersion());
        assertEquals("2502", m.deviceEventClassId());
        assertEquals("User missed the password more than one time", m.name());
        assertEquals(10, m.severity().numeric());
        assertEquals("Very-High", m.severity().text());

        assertNull(m.message());

        assertEquals("ip-172-30-2-212", m.fields().get("dvc"));
        assertEquals(22, m.fields().get("spt"));
        assertEquals(90.01F, m.fields().get("SomeFloat"));
        assertEquals("ip-172-30-2-212->/var/log/auth.log", m.fields().get("Location"));
    }

    @Test
    public void testParseWithEscapedPipe_V24() throws Exception {
        // "If a pipe (|) is used in the header, it has to be escaped with a backslash (\). But note that pipes in the extension do not need escaping."
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|detected a \\| in message|10|src=10.0.0.1 act=blocked a | dst=1.1.1.1").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("detected a | in message", m.name());
        assertEquals(10, m.severity().numeric());
        assertEquals("Very-High", m.severity().text());

        assertNull(m.message());

        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("1.1.1.1", m.fields().get("dst"));
    }

    @Test
    public void testParseWithEscapedBackslash_V24() throws Exception {
        // "If a backslash (\) is used in the header or the extension, it has to be escaped with another backslash (\)."
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|detected a \\\\ in packet|10|src=10.0.0.1 act=blocked a \\\\ dst=1.1.1.1\n").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("detected a \\ in packet", m.name());
        assertEquals(10, m.severity().numeric());
        assertEquals("Very-High", m.severity().text());

        assertNull(m.message());

        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("1.1.1.1", m.fields().get("dst"));
        assertEquals("blocked a \\", m.fields().get("act"));
    }

    @Test
    public void testParseWithEscapedEqualSign_V24() throws Exception {
        // "If an equal sign (=) is used in the extensions, it has to be escaped with a backslash (\). Equal signs in the header need no escaping."
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|detected a = in message|10|src=10.0.0.1 act=blocked a \\= dst=1.1.1.1").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("detected a = in message", m.name());
        assertEquals(10, m.severity().numeric());
        assertEquals("Very-High", m.severity().text());

        assertNull(m.message());

        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("1.1.1.1", m.fields().get("dst"));
        assertEquals("blocked a =", m.fields().get("act"));
    }

    @Test
    public void testParseWithMultiLine_V24() throws Exception {
        // "Multi-line fields can be sent by CEF by encoding the newline character as \n or \r. Note that multiple lines are only allowed in the value part of the extensions."
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|Detected a threat. No action needed.|10|src=10.0.0.1 custom=foo\\rbar msg=Detected a threat.\\nNo action needed.").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("Detected a threat. No action needed.", m.name());
        assertEquals(10, m.severity().numeric());
        assertEquals("Very-High", m.severity().text());
        assertEquals("Detected a threat.\nNo action needed.", m.message());
        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("foo\rbar", m.fields().get("custom"));
    }

    @Test
    public void testNumericSeverity() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(i, parser.parse("CEF:0|vendor|product|1.0|id|name|" + i + "|msg=Foobar").build().severity().numeric());
        }
    }

    @Test
    public void testTextualSeverity() throws Exception {
        assertEquals(0, parser.parse("CEF:0|vendor|product|1.0|id|name|low|msg=Foobar").build().severity().numeric());
        assertEquals(0, parser.parse("CEF:0|vendor|product|1.0|id|name|LOW|msg=Foobar").build().severity().numeric());
        assertEquals(4, parser.parse("CEF:0|vendor|product|1.0|id|name|medium|msg=Foobar").build().severity().numeric());
        assertEquals(4, parser.parse("CEF:0|vendor|product|1.0|id|name|MEDIUM|msg=Foobar").build().severity().numeric());
        assertEquals(7, parser.parse("CEF:0|vendor|product|1.0|id|name|high|msg=Foobar").build().severity().numeric());
        assertEquals(7, parser.parse("CEF:0|vendor|product|1.0|id|name|HIGH|msg=Foobar").build().severity().numeric());
        assertEquals(9, parser.parse("CEF:0|vendor|product|1.0|id|name|very-high|msg=Foobar").build().severity().numeric());
        assertEquals(9, parser.parse("CEF:0|vendor|product|1.0|id|name|VERY-HIGH|msg=Foobar").build().severity().numeric());
        assertEquals(9, parser.parse("CEF:0|vendor|product|1.0|id|name|Very-High|msg=Foobar").build().severity().numeric());
        assertEquals(9, parser.parse("CEF:0|vendor|product|1.0|id|name|Very-High|msg=Foobar").build().severity().numeric());
        assertEquals(9, parser.parse("CEF:0|vendor|product|1.0|id|name|Very-High|msg=Foobar").build().severity().numeric());
        assertEquals(9, parser.parse("CEF:0|vendor|product|1.0|id|name|Very-High|msg=Foobar").build().severity().numeric());
        assertEquals(-1, parser.parse("CEF:0|vendor|product|1.0|id|name|unknown|msg=Foobar").build().severity().numeric());
        assertEquals(-1, parser.parse("CEF:0|vendor|product|1.0|id|name|UNKNOWN|msg=Foobar").build().severity().numeric());
        assertEquals(-1, parser.parse("CEF:0|vendor|product|1.0|id|name|FOOBAR|msg=Foobar").build().severity().numeric());
    }
}