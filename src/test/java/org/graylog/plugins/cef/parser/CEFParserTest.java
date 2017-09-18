package org.graylog.plugins.cef.parser;

import org.junit.Test;

import static org.junit.Assert.*;

public class CEFParserTest {

    @Test
    public void testParse() throws Exception {
        CEFParser parser = new CEFParser();
        CEFMessage m = parser.parse("CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cfp2=90.01 cfp2Label=SomeFloat spt=22 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: PAM 2 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=116.31.116.17  user=root").build();

        assertEquals(0, m.version());
        assertEquals("Trend Micro Inc.", m.deviceVendor());
        assertEquals("OSSEC HIDS", m.deviceProduct());
        assertEquals("v2.8.3", m.deviceVersion());
        assertEquals("2502", m.deviceEventClassId());
        assertEquals("User missed the password more than one time", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());

        assertEquals("Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: PAM 2 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=116.31.116.17  user=root", m.message());

        assertEquals("ip-172-30-2-212", m.fields().get("dvc"));
        assertEquals(22, m.fields().get("spt"));
        assertEquals(90.01F, m.fields().get("SomeFloat"));
        assertEquals("ip-172-30-2-212->/var/log/auth.log", m.fields().get("Location"));
    }

    @Test
    public void testParseWithMissingMsgField() throws Exception {
        CEFParser parser = new CEFParser();
        CEFMessage m = parser.parse("CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location cfp2=90.01 cfp2Label=SomeFloat spt=22 ").build();

        assertEquals(0, m.version());
        assertEquals("Trend Micro Inc.", m.deviceVendor());
        assertEquals("OSSEC HIDS", m.deviceProduct());
        assertEquals("v2.8.3", m.deviceVersion());
        assertEquals("2502", m.deviceEventClassId());
        assertEquals("User missed the password more than one time", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());

        assertNull(m.message());

        assertEquals("ip-172-30-2-212", m.fields().get("dvc"));
        assertEquals(22, m.fields().get("spt"));
        assertEquals(90.01F, m.fields().get("SomeFloat"));
        assertEquals("ip-172-30-2-212->/var/log/auth.log", m.fields().get("Location"));
    }

    @Test
    public void testParseWithEscapedPipe_V24() throws Exception {
        // "If a pipe (|) is used in the header, it has to be escaped with a backslash (\). But note that pipes in the extension do not need escaping."
        CEFParser parser = new CEFParser();
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|detected a \\| in message|10|src=10.0.0.1 act=blocked a | dst=1.1.1.1").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("detected a | in message", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());

        assertNull(m.message());

        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("1.1.1.1", m.fields().get("dst"));
    }

    @Test
    public void testParseWithEscapedBackslash_V24() throws Exception {
        // "If a backslash (\) is used in the header or the extension, it has to be escaped with another backslash (\)."
        CEFParser parser = new CEFParser();
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|detected a \\\\ in packet|10|src=10.0.0.1 act=blocked a \\\\ dst=1.1.1.1\n").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("detected a \\ in packet", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());

        assertNull(m.message());

        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("1.1.1.1", m.fields().get("dst"));
        assertEquals("blocked a \\", m.fields().get("act"));
    }

    @Test
    public void testParseWithEscapedEqualSign_V24() throws Exception {
        // "If an equal sign (=) is used in the extensions, it has to be escaped with a backslash (\). Equal signs in the header need no escaping."
        CEFParser parser = new CEFParser();
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|detected a = in message|10|src=10.0.0.1 act=blocked a \\= dst=1.1.1.1").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("detected a = in message", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());

        assertNull(m.message());

        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("1.1.1.1", m.fields().get("dst"));
        assertEquals("blocked a =", m.fields().get("act"));
    }

    @Test
    public void testParseWithMultiLine_V24() throws Exception {
        // "Multi-line fields can be sent by CEF by encoding the newline character as \n or \r. Note that multiple lines are only allowed in the value part of the extensions."
        CEFParser parser = new CEFParser();
        CEFMessage m = parser.parse("CEF:0|security|threatmanager|1.0|100|Detected a threat. No action needed.|10|src=10.0.0.1 custom=foo\\rbar msg=Detected a threat.\\nNo action needed.").build();

        assertEquals(0, m.version());
        assertEquals("security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("Detected a threat. No action needed.", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());
        assertEquals("Detected a threat.\nNo action needed.", m.message());
        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("foo\rbar", m.fields().get("custom"));
    }
}