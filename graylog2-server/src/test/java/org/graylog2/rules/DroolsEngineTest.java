/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rules;

import com.google.common.collect.Sets;
import org.graylog2.Graylog2BaseTest;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.testng.annotations.Test;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DroolsEngineTest extends Graylog2BaseTest {

    @Test
    public void runWithoutRules() {
        final DroolsEngine engine = new DroolsEngine(Sets.<URL>newHashSet());

        final int rulesFired = engine.evaluateInSharedSession(new Message("test message", "test", Tools.iso8601()));

        assertEquals(rulesFired, 0, "No rules should have fired");

        engine.stop();
    }

    @Test
    public void addedRuleIsVisibleInSession() {
        final DroolsEngine engine = new DroolsEngine(Sets.<URL>newHashSet());

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
        assertTrue(valid1, "Rule should compile without errors");

        final boolean valid2 = engine.addRule(rule2);
        assertTrue(valid2, "Rule should compile without errors");

        final Message msg = new Message("test message", "test source", Tools.iso8601());
        final int fired = engine.evaluateInSharedSession(msg);

        assertTrue(msg.getFilterOut(), "msg is filtered out");
        assertEquals(fired, 2, "both rules should have fired");

        engine.stop();
    }

    @Test
    public void incorrectRuleIsNotApplied() {
        final DroolsEngine engine = new DroolsEngine(Sets.<URL>newHashSet());

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
        assertFalse(deployed, "Should not deploy invalid rule");

        deployed = engine.addRule(validRule);
        assertTrue(deployed, "Subsequent deployment of valid rule works");

        engine.evaluateInSharedSession(new Message("foo", "source", Tools.iso8601()));

        engine.stop();
    }
}