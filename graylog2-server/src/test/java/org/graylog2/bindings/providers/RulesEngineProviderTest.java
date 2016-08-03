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
package org.graylog2.bindings.providers;

import org.graylog2.rules.DroolsEngine;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RulesEngineProviderTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private DroolsEngine rulesEngine;

    @Test(expected = NullPointerException.class)
    public void providerThrowsNullPointerExceptionIfDroolsEngineIsNull() throws Exception {
        new RulesEngineProvider(null, "test");
    }

    @Test
    public void providerReturnsRulesEngineWithAddedRule() throws Exception {
        when(rulesEngine.addRulesFromFile("test")).thenReturn(true);
        final RulesEngineProvider provider = new RulesEngineProvider(rulesEngine, "test");
        verify(rulesEngine, only()).addRulesFromFile("test");
        assertThat(provider.get()).isSameAs(rulesEngine);
    }

    @Test
    public void providerReturnsRulesEngineWithoutAddedRuleIfAddingRulesFromFileFailed() throws Exception {
        when(rulesEngine.addRulesFromFile("test")).thenReturn(false);
        final RulesEngineProvider provider = new RulesEngineProvider(rulesEngine, "test");
        verify(rulesEngine, only()).addRulesFromFile("test");
        assertThat(provider.get()).isSameAs(rulesEngine);
    }

    @Test
    public void providerReturnsRulesEngineWithoutAddedRuleIfRulesFilePathIsNull() throws Exception {
        final RulesEngineProvider provider = new RulesEngineProvider(rulesEngine, null);
        verify(rulesEngine, never()).addRulesFromFile(null);
        assertThat(provider.get()).isSameAs(rulesEngine);
    }

    @Test
    public void providerReturnsRulesEngineWithoutAddedRuleIfRulesFilePathIsEmpty() throws Exception {
        final RulesEngineProvider provider = new RulesEngineProvider(rulesEngine, "");
        verify(rulesEngine, never()).addRulesFromFile(anyString());
        assertThat(provider.get()).isSameAs(rulesEngine);
    }
}
