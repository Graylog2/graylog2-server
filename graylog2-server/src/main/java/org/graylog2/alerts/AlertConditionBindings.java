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
package org.graylog2.alerts;

import org.graylog2.alerts.types.FieldContentValueAlertCondition;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.plugin.PluginModule;

public class AlertConditionBindings extends PluginModule {
    @Override
    protected void configure() {
        addAlertCondition(AbstractAlertCondition.Type.FIELD_CONTENT_VALUE.toString(),
            FieldContentValueAlertCondition.class,
            FieldContentValueAlertCondition.Factory.class);
        addAlertCondition(AbstractAlertCondition.Type.FIELD_VALUE.toString(),
            FieldValueAlertCondition.class,
            FieldValueAlertCondition.Factory.class);
        addAlertCondition(AbstractAlertCondition.Type.MESSAGE_COUNT.toString(),
            MessageCountAlertCondition.class,
            MessageCountAlertCondition.Factory.class);
    }
}
