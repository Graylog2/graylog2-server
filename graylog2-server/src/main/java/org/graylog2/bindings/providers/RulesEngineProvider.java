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

import org.graylog2.plugin.RulesEngine;
import org.graylog2.rules.DroolsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@Singleton
public class RulesEngineProvider implements Provider<RulesEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(RulesEngineProvider.class);
    private final RulesEngine rulesEngine;

    @Inject
    public RulesEngineProvider(DroolsEngine droolsEngine,
                               @Named("rules_file") @Nullable String rulesFilePath) {
        this.rulesEngine = checkNotNull(droolsEngine);

        if (!isNullOrEmpty(rulesFilePath)) {
            if (rulesEngine.addRulesFromFile(rulesFilePath)) {
                LOG.info("Using rules: {}", rulesFilePath);
            } else {
                LOG.warn("Unable to load rules due to load error: {}", rulesFilePath);
            }
        } else {
            LOG.info("No static rules file loaded.");
        }
    }

    @Override
    public RulesEngine get() {
        return rulesEngine;
    }
}
