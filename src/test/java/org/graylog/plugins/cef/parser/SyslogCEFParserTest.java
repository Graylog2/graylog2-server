package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SyslogCEFParserTest {

    @Test
    public void testParse() throws Exception {
        int year = DateTime.now(DateTimeZone.getDefault()).getYear();

        SyslogCEFParser parser = new SyslogCEFParser(DateTimeZone.UTC);
        CEFMessage m = parser.parse("<132>Aug 14 14:26:55 CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cfp2=90.01 cfp2Label=SomeFloat spt=22 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: PAM 2 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=116.31.116.17  user=root");

        DateTime timestamp = m.timestamp().withZone(DateTimeZone.UTC);

        // THIS WILL BREAK ON NEW YEARS EVE FOR A MOMENT and I don't care
        assertEquals(year, timestamp.getYear());
        assertEquals(8, timestamp.getMonthOfYear());
        assertEquals(14, timestamp.getDayOfMonth());
        assertEquals(14, timestamp.getHourOfDay());
        assertEquals(26, timestamp.getMinuteOfHour());
        assertEquals(55, timestamp.getSecondOfMinute());

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
        int year = DateTime.now(DateTimeZone.getDefault()).getYear();

        SyslogCEFParser parser = new SyslogCEFParser(DateTimeZone.UTC);
        CEFMessage m = parser.parse("<132>Aug 14 14:26:55 CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location cfp2=90.01 cfp2Label=SomeFloat spt=22 ");

        DateTime timestamp = m.timestamp().withZone(DateTimeZone.UTC);

        // THIS WILL BREAK ON NEW YEARS EVE FOR A MOMENT and I don't care
        assertEquals(year, timestamp.getYear());
        assertEquals(8, timestamp.getMonthOfYear());
        assertEquals(14, timestamp.getDayOfMonth());
        assertEquals(14, timestamp.getHourOfDay());
        assertEquals(26, timestamp.getMinuteOfHour());
        assertEquals(55, timestamp.getSecondOfMinute());

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
    public void testParseUsesProvidedTimezone() throws Exception {
        SyslogCEFParser parser = new SyslogCEFParser(DateTimeZone.UTC);
        CEFMessage m = parser.parse("<132>Aug 14 14:26:55 CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location cfp2=90.01 cfp2Label=SomeFloat spt=22 ");

        assertEquals("UTC", m.timestamp().getZone().toString());

        SyslogCEFParser parser2 = new SyslogCEFParser(DateTimeZone.forID("+01:00"));
        CEFMessage m2 = parser2.parse("<132>Aug 14 14:26:55 CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location cfp2=90.01 cfp2Label=SomeFloat spt=22 ");

        assertEquals("+01:00", m2.timestamp().getZone().toString());
    }

    @Test
    public void testParseLeftPaddedDate() throws Exception {
        SyslogCEFParser parser = new SyslogCEFParser(DateTimeZone.UTC);
        CEFMessage m = parser.parse("<132>Aug  4 14:26:55 CEF:0|Trend Micro Inc.|OSSEC HIDS|v2.8.3|2502|User missed the password more than one time|10|dvc=ip-172-30-2-212 cfp2=90.01 cfp2Label=SomeFloat spt=22 cs2=ip-172-30-2-212->/var/log/auth.log cs2Label=Location msg=Aug 14 14:26:53 ip-172-30-2-212 sshd[16217]: PAM 2 more authentication failures; logname= uid=0 euid=0 tty=ssh ruser= rhost=116.31.116.17  user=root");
    }

    @Test
    public void testCommonEventFormat_V24() throws Exception {
        SyslogCEFParser parser = new SyslogCEFParser(DateTimeZone.UTC);
        // CEF Implementation -> Header Information
        CEFMessage m = parser.parse("<132>Sep 19 08:26:10 host CEF:0|Security|threatmanager|1.0|100|worm successfully stopped|10|src=10.0.0.1 dst=2.1.2.2 spt=1232");
        assertNotNull(m);
        assertEquals("host", m.hostname());
        assertEquals(0, m.version());
        assertEquals("Security", m.deviceVendor());
        assertEquals("threatmanager", m.deviceProduct());
        assertEquals("1.0", m.deviceVersion());
        assertEquals("100", m.deviceEventClassId());
        assertEquals("worm successfully stopped", m.name());
        assertEquals(10, m.severity());
        assertEquals("VERY HIGH", m.humanReadableSeverity());
        assertEquals("10.0.0.1", m.fields().get("src"));
        assertEquals("2.1.2.2", m.fields().get("dst"));
        assertEquals(1232, m.fields().get("spt"));
        assertNull(m.message());
    }

}