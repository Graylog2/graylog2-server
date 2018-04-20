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
package org.graylog2.decorators;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.plugin.inject.Graylog2Module;

public class DecoratorBindings extends Graylog2Module {
    @Override
    protected void configure() {
        final MapBinder<String, SearchResponseDecorator.Factory> searchResponseDecoratorBinder = searchResponseDecoratorBinder();
        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       SyslogSeverityMapperDecorator.class,
                                       SyslogSeverityMapperDecorator.Factory.class);

        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       FormatStringDecorator.class,
                                       FormatStringDecorator.Factory.class);

        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       LookupTableDecorator.class,
                                       LookupTableDecorator.Factory.class);

        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       LinkFieldDecorator.class,
                                       LinkFieldDecorator.Factory.class);
    }
}
