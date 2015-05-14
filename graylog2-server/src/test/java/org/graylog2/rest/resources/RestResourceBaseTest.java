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
package org.graylog2.rest.resources;

import com.google.inject.Module;
import org.assertj.core.util.Lists;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;

import java.util.List;

public class RestResourceBaseTest {
    @Before
    public void setUpInjector() throws Exception {
        // The list of modules is empty for now so only JIT injection will be used.
        final List<Module> modules = Lists.emptyList();
        GuiceInjectorHolder.createInjector(modules);
    }
}
