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
package org.graylog2.shared.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.periodical.Periodical;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PeriodicalBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        Reflections reflections = new Reflections("org.graylog2.periodical");
        for (Class<? extends Periodical> periodicalClass : reflections.getSubTypesOf(Periodical.class))
            if (!Modifier.isAbstract(periodicalClass.getModifiers()))
                periodicalBinder.addBinding().to(periodicalClass);
    }
}
