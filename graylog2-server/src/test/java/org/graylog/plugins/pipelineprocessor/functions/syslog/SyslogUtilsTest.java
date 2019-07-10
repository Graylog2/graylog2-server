package org.graylog.plugins.pipelineprocessor.functions.syslog;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class SyslogUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  /* testedClasses: SyslogUtils */
  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull() {

    // Arrange
    final int facility = 18;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local2", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull10() {

    // Arrange
    final int facility = 8;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("uucp", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull11() {

    // Arrange
    final int facility = 12;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("ntp", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull12() {

    // Arrange
    final int facility = 22;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local6", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull13() {

    // Arrange
    final int facility = 6;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("lpr", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull14() {

    // Arrange
    final int facility = 5;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("syslog", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull15() {

    // Arrange
    final int facility = 17;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local1", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull16() {

    // Arrange
    final int facility = 15;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("cron", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull17() {

    // Arrange
    final int facility = 14;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("log alert", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull18() {

    // Arrange
    final int facility = 13;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("log audit", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull19() {

    // Arrange
    final int facility = 11;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("ftp", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull2() {

    // Arrange
    final int facility = 19;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local3", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull20() {

    // Arrange
    final int facility = 7;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("news", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull21() {

    // Arrange
    final int facility = 4;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("auth", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull22() {

    // Arrange
    final int facility = 3;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("daemon", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull23() {

    // Arrange
    final int facility = 2;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("mail", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull24() {

    // Arrange
    final int facility = 1;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("user", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull3() {

    // Arrange
    final int facility = 23;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local7", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull4() {

    // Arrange
    final int facility = 20;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local4", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull5() {

    // Arrange
    final int facility = 10;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("authpriv", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull6() {

    // Arrange
    final int facility = 16;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local0", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull7() {

    // Arrange
    final int facility = 9;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("clock", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull8() {

    // Arrange
    final int facility = 21;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("local5", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputPositiveOutputNotNull9() {

    // Arrange
    final int facility = 2069;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("Unknown", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void facilityToStringInputZeroOutputNotNull() {

    // Arrange
    final int facility = 0;

    // Act
    final String actual = SyslogUtils.facilityToString(facility);

    // Assert result
    Assert.assertEquals("kern", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelFromPriorityInputPositiveOutputZero() {

    // Arrange
    final int priority = 8;

    // Act
    final int actual = SyslogUtils.levelFromPriority(priority);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull() {

    // Arrange
    final int level = 131_073;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Unknown", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull2() {

    // Arrange
    final int level = 2;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Critical", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull3() {

    // Arrange
    final int level = 6;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Informational", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull4() {

    // Arrange
    final int level = 7;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Debug", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull5() {

    // Arrange
    final int level = 5;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Notice", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull6() {

    // Arrange
    final int level = 4;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Warning", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull7() {

    // Arrange
    final int level = 3;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Error", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputPositiveOutputNotNull8() {

    // Arrange
    final int level = 1;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Alert", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void levelToStringInputZeroOutputNotNull() {

    // Arrange
    final int level = 0;

    // Act
    final String actual = SyslogUtils.levelToString(level);

    // Assert result
    Assert.assertEquals("Emergency", actual);
  }
}
