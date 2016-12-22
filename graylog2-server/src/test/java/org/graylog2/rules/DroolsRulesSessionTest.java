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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DroolsRulesSessionTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private KieSession kieSession;
    @Mock
    private FactHandle factHandle;

    private Message message;
    private DroolsRulesSession session;
    private int rulesFired = 3;

    @Before
    public void setUp() throws Exception {
        message = new Message("hello", "localhost", Tools.nowUTC());

        when(kieSession.insert(message)).thenReturn(factHandle);
        when(kieSession.fireAllRules()).thenReturn(rulesFired);

        session = new DroolsRulesSession(kieSession);
    }

    @Test
    public void testClose() throws Exception {
        session.close();

        verify(kieSession).dispose();
    }

    @Test
    public void testEvaluateWithRetractFacts() throws Exception {
        final int i = session.evaluate(message, true);

        verify(kieSession).insert(message);
        verify(kieSession).fireAllRules();
        verify(kieSession).delete(factHandle);

        assertEquals(i, rulesFired);
    }

    @Test
    public void testEvaluateWithoutRetractFacts() throws Exception {
        final int i = session.evaluate(message, false);

        verify(kieSession).insert(message);
        verify(kieSession).fireAllRules();
        verify(kieSession, never()).delete(factHandle);

        assertEquals(i, rulesFired);
    }

    @Test(expected = IllegalStateException.class)
    public void testEvaluateWithNullSession() throws Exception {
        DroolsRulesSession session = new DroolsRulesSession(null);

        session.evaluate(message, false);
    }

    @Test(expected = IllegalStateException.class)
    public void testEvaluateWithClosedSession() throws Exception {
        session.close();
        session.evaluate(message, false);
    }

    @Test
    public void testInsertFact() throws Exception {
        final Object o = new Object();

        session.insertFact(o);

        verify(kieSession).insert(o);
    }

    @Test
    public void testDeleteFactWithFactHandleForFact() throws Exception {
        final Object fact = new Object();

        when(kieSession.getFactHandle(fact)).thenReturn(factHandle);

        assertTrue(session.deleteFact(fact));
        verify(kieSession).delete(factHandle);
    }

    @Test
    public void testDeleteFactWithoutFactHandleForFact() throws Exception {
        final Object fact = new Object();

        when(kieSession.getFactHandle(fact)).thenReturn(null);

        assertFalse(session.deleteFact(fact));
        verify(kieSession, never()).delete(factHandle);
    }
}
