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

import org.graylog2.plugin.Message;
import org.graylog2.plugin.RulesEngine;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.io.IOException;

public class DroolsRulesSession implements RulesEngine.RulesSession {
    private KieSession kieSession;

    public DroolsRulesSession(KieSession kieSession) {
        this.kieSession = kieSession;
    }

    @Override
    public void close() throws IOException {
        kieSession.dispose();
        kieSession = null;
    }

    @Override
    public int evaluate(Message message, boolean retractFacts) {
        if (kieSession == null) {
            throw new IllegalStateException("Session already disposed");
        }
        final FactHandle handle = kieSession.insert(message);
        final int rulesFired = kieSession.fireAllRules();
        if (retractFacts) {
            kieSession.delete(handle);
        }
        return rulesFired;
    }

    @Override
    public Object insertFact(Object fact) {
        return kieSession.insert(fact);
    }

    @Override
    public boolean deleteFact(Object fact) {
        final FactHandle factHandle = kieSession.getFactHandle(fact);
        if (factHandle == null) {
            return false;
        }
        kieSession.delete(factHandle);
        return true;
    }
}
