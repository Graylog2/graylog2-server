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
package org.graylog.integrations.inputs.paloalto;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

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