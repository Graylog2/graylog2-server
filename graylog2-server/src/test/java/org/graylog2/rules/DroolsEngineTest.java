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
package org.graylog2.rules;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DroolsEngineTest {

    @Test
    public void runWithoutRules() {
        final DroolsEngine engine = new DroolsEngine(Collections.<URI>emptySet());

        final int rulesFired = engine.evaluateInSharedSession(new Message("test message", "test", Tools.nowUTC()));

        assertEquals("No rules should have fired", rulesFired, 0);

        engine.stop();
    }

    @Test
    public void addedRuleIsVisibleInSession() {
        final DroolsEngine engine = new DroolsEngine(Collections.<URI>emptySet());

        String rule1 =
                "declare Message\n" +
                "    @role( event )\n" +
                "end\n" +
                "\n" +
                "rule \"filter out all messages\"\n" +
                "when\n" +
                "    $m : Message( filterOut == false )\n" +
                "then\n" +
                "    modify($m) { setFilterOut(true) };\n" +
                "    log.info(\"filtering out message from \" + $m.getSource());\n" +
                "end\n";
        String rule2 =
                "declare Message\n" +
                "    @role( event )\n" +
                "end\n" +
                "\n" +
                "rule \"print filtered out message source\"\n" +
                "when\n" +
                "    $m : Message( filterOut == true )\n" +
                "then\n" +
                "    log.info(\"message from \" + $m.getSource() + \" filtered out\");\n" +
                "end\n";

        final boolean valid1 = engine.addRule(rule1);
        assertTrue("Rule should compile without errors", valid1);

        final boolean valid2 = engine.addRule(rule2);
        assertTrue("Rule should compile without errors", valid2);

        final Message msg = new Message("test message", "test source", Tools.nowUTC());
        final int fired = engine.evaluateInSharedSession(msg);

        assertTrue("msg is filtered out", msg.getFilterOut());
        assertEquals("both rules should have fired", fired, 2);

        engine.stop();
    }

    @Test
    public void incorrectRuleIsNotApplied() {
        final DroolsEngine engine = new DroolsEngine(Collections.<URI>emptySet());

        String invalidRule = "rule \"this will not compile\"\n" +
                "when\n" +
                "then\n" +
                "ende";

        String validRule = "rule \"this will compile\"\n" +
                "when\n" +
                "  exists (Object())\n" +
                "then\n" +
                "  log.info(\"Found some object\");\n" +
                "end";

        boolean deployed = engine.addRule(invalidRule);
        assertFalse("Should not deploy invalid rule", deployed);

        deployed = engine.addRule(validRule);
        assertTrue("Subsequent deployment of valid rule works", deployed);

        engine.evaluateInSharedSession(new Message("foo", "source", Tools.nowUTC()));

        engine.stop();
    }
}
