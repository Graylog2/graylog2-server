package views.helpers;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class DateHelperTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @BeforeClass
    public static void setUp() {
        Locale.setDefault(Locale.ENGLISH);
        DateTimeUtils.setCurrentMillisFixed(0L);
    }

    @AfterClass
    public static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testCurrent() throws Exception {
        assertThat(DateHelper.current().body())
                .contains("1970-01-01T00:00:00.000Z")
                .contains("Thu Jan 01 1970 00:00:00.000 +00:00")
                .endsWith("</time>");
    }

    @Test
    public void testTimestamp() throws Exception {
        assertThat(DateHelper.timestamp(null).body())
                .isEmpty();
        assertThat(DateHelper.timestamp(DateTime.now(DateTimeZone.UTC)).body())
                .contains("1970-01-01T00:00:00.000Z")
                .endsWith("</time>");
    }

    @Test
    public void testTimestampShort() throws Exception {
        assertThat(DateHelper.timestampShort(null).body())
                .isEmpty();
        assertThat(DateHelper.timestampShort(DateTime.now(DateTimeZone.UTC)).body())
                .contains("1970-01-01T00:00:00.000Z")
                .endsWith("</time>");
    }

    @Test
    public void testTimestampShortTZ() throws Exception {
        assertThat(DateHelper.timestampShortTZ(null).body())
                .isEmpty();
        assertThat(DateHelper.timestampShortTZ(null, true).body())
                .isEmpty();
        assertThat(DateHelper.timestampShortTZ(null, false).body())
                .isEmpty();
        assertThat(DateHelper.timestampShortTZ(DateTime.now(DateTimeZone.UTC), true).body())
                .contains("1970-01-01T00:00:00.000Z")
                .endsWith("</time>");
        assertThat(DateHelper.timestampShortTZ(DateTime.now(DateTimeZone.UTC), false).body())
                .contains("1970-01-01T00:00:00.000Z")
                .endsWith("</time>");
    }

    @Test
    public void testReadablePeriodFromNow() throws Exception {
        assertThat(DateHelper.readablePeriodFromNow(null).body())
                .isEmpty();
        assertThat(DateHelper.readablePeriodFromNow(DateTime.now(DateTimeZone.UTC)).body())
                .contains("1970-01-01T00:00:00.000Z")
                .endsWith("</time>");
    }

    @Test
    public void testReadableDuration() throws Exception {
        assertThat(DateHelper.readableDuration(null).body())
                .isEmpty();
        assertThat(DateHelper.readableDuration(Duration.standardHours(1L)).body())
                .contains("1 hour")
                .endsWith("</time>");
    }
}