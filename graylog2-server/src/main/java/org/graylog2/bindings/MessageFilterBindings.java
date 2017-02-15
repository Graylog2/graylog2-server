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
package org.graylog2.bindings;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.filters.ExtractorFilter;
import org.graylog2.filters.RulesFilter;
import org.graylog2.filters.StaticFieldFilter;
import org.graylog2.filters.StreamMatcherFilter;
import org.graylog2.plugin.filters.MessageFilter;

import java.net.URI;
import java.net.URISyntaxException;

public class MessageFilterBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<MessageFilter> messageFilters = Multibinder.newSetBinder(binder(), MessageFilter.class);
        messageFilters.addBinding().to(StaticFieldFilter.class);
        messageFilters.addBinding().to(ExtractorFilter.class);
        messageFilters.addBinding().to(RulesFilter.class);
        messageFilters.addBinding().to(StreamMatcherFilter.class);

        // built it drools rules
        final Multibinder<URI> rulesUrls = Multibinder.newSetBinder(binder(), URI.class);
        try {
            final URI blacklistRulesUri = Resources.getResource("blacklist.drl").toURI();
            rulesUrls.addBinding().toInstance(blacklistRulesUri);
        } catch (URISyntaxException ignored) {
            // Ignore
        }
    }
}
