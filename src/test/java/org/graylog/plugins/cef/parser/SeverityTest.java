package org.graylog.plugins.cef.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SeverityTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"", CEFMessage.Severity.UNKNOWN},
                {"   ", CEFMessage.Severity.UNKNOWN},
                {"foobar", CEFMessage.Severity.UNKNOWN},
                {"Unknown", CEFMessage.Severity.UNKNOWN},
                {"0", CEFMessage.Severity.create(0, "Low")},
                {"1", CEFMessage.Severity.create(1, "Low")},
                {"2", CEFMessage.Severity.create(2, "Low")},
                {"3", CEFMessage.Severity.create(3, "Low")},
                {" 3 ", CEFMessage.Severity.create(3, "Low")},
                {"Low", CEFMessage.Severity.create(0, "Low")},
                {"LOW", CEFMessage.Severity.create(0, "Low")},
                {"low", CEFMessage.Severity.create(0, "Low")},
                {" Low ", CEFMessage.Severity.create(0, "Low")},
                {"4", CEFMessage.Severity.create(4, "Medium")},
                {"5", CEFMessage.Severity.create(5, "Medium")},
                {"6", CEFMessage.Severity.create(6, "Medium")},
                {" 6 ", CEFMessage.Severity.create(6, "Medium")},
                {"Medium", CEFMessage.Severity.create(4, "Medium")},
                {"MEDIUM", CEFMessage.Severity.create(4, "Medium")},
                {"medium", CEFMessage.Severity.create(4, "Medium")},
                {" Medium ", CEFMessage.Severity.create(4, "Medium")},
                {"7", CEFMessage.Severity.create(7, "High")},
                {"8", CEFMessage.Severity.create(8, "High")},
                {" 8 ", CEFMessage.Severity.create(8, "High")},
                {"High", CEFMessage.Severity.create(7, "High")},
                {"HIGH", CEFMessage.Severity.create(7, "High")},
                {"high", CEFMessage.Severity.create(7, "High")},
                {" High ", CEFMessage.Severity.create(7, "High")},
                {"9", CEFMessage.Severity.create(9, "Very-High")},
                {"10", CEFMessage.Severity.create(10, "Very-High")},
                {" 10 ", CEFMessage.Severity.create(10, "Very-High")},
                {"Very-High", CEFMessage.Severity.create(9, "Very-High")},
                {"VERY-HIGH", CEFMessage.Severity.create(9, "Very-High")},
                {"very-high", CEFMessage.Severity.create(9, "Very-High")},
                {" Very-High ", CEFMessage.Severity.create(9, "Very-High")},
        });
    }

    private final String text;
    private final CEFMessage.Severity expectedSeverity;

    public SeverityTest(String text, CEFMessage.Severity expectedSeverity) {
        this.text = text;
        this.expectedSeverity = expectedSeverity;
    }

    @Test
    public void parse() throws Exception {
        assertEquals(text, expectedSeverity, CEFMessage.Severity.parse(text));
    }
}