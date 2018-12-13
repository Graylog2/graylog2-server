package org.graylog.integrations.inputs.paloalto;

import org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults;
import org.graylog.integrations.inputs.paloalto.PaloAltoTemplates;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test parsing of raw PAN message templates.
 */
public class PaloAltoTemplateTest {

    public static final String DEFAULT_HEADER = "field,position,type";

    @Test
    public void parseTest() throws Exception {

        PaloAltoTemplates builder = PaloAltoTemplates.newInstance(PaloAltoTemplateDefaults.SYSTEM_TEMPLATE,
                                                                  PaloAltoTemplateDefaults.THREAT_TEMPLATE,
                                                                  PaloAltoTemplateDefaults.TRAFFIC_TEMPLATE);

        // Verify that the correct number of fields were parsed.
        assertEquals(22, builder.getSystemMessageTemplate().getFields().size());
        assertEquals(74, builder.getThreatMessageTemplate().getFields().size());
        assertEquals(64, builder.getTrafficMessageTemplate().getFields().size());
    }

    @Test
    public void verifyCSVValidation() {

        // Verify header checking.
        PaloAltoTemplates templates = PaloAltoTemplates.newInstance("badheader",
                                                                    DEFAULT_HEADER,
                                                                    DEFAULT_HEADER);
        assertEquals(3, templates.getAllErrors().size());
        templates.getAllErrors().forEach(error -> {
            assertTrue(error.contains("The header row is invalid"));
        });

        // Verify that invalid value messages returned for invalid values.
        templates = PaloAltoTemplates.newInstance("field,position,type\n" +
                                                  "badvalue",
                                                  DEFAULT_HEADER,
                                                  DEFAULT_HEADER);

        templates.getAllErrors().forEach(error -> {
            assertTrue(error.contains("[] is not a valid"));
        });
    }
}