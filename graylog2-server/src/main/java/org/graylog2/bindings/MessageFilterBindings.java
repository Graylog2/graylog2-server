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
package org.graylog2.bindings;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.filters.ExtractorFilter;
import org.graylog2.filters.RulesFilter;
import org.graylog2.filters.StaticFieldFilter;
import org.graylog2.filters.StreamMatcherFilter;
import org.graylog2.plugin.filters.MessageFilter;

import java.net.URL;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageFilterBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<MessageFilter> messageFilters = Multibinder.newSetBinder(binder(), MessageFilter.class);
        messageFilters.addBinding().to(StaticFieldFilter.class);
        messageFilters.addBinding().to(ExtractorFilter.class);
        messageFilters.addBinding().to(RulesFilter.class);
        messageFilters.addBinding().to(StreamMatcherFilter.class);

        // built it drools rules
        final Multibinder<URL> rulesUrls = Multibinder.newSetBinder(binder(), URL.class);
        rulesUrls.addBinding().toInstance(Resources.getResource("blacklist.drl"));
    }
}
