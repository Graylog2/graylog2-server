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

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alarmcallbacks.HTTPAlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<AlarmCallback> alarmCallbackBinder = Multibinder.newSetBinder(binder(), AlarmCallback.class);
        alarmCallbackBinder.addBinding().to(EmailAlarmCallback.class);
        alarmCallbackBinder.addBinding().to(HTTPAlarmCallback.class);

        TypeLiteral<Class<? extends AlarmCallback>> type = new TypeLiteral<Class<? extends AlarmCallback>>(){};
        Multibinder<Class<? extends AlarmCallback>> alarmCallbackClassBinder = Multibinder.newSetBinder(binder(), type);
        alarmCallbackClassBinder.addBinding().toInstance(EmailAlarmCallback.class);
        alarmCallbackClassBinder.addBinding().toInstance(HTTPAlarmCallback.class);
    }
}
