package org.graylog2.alerts;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.alerts.types.FieldContentValueAlertCondition;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.inject.Graylog2Module;

public class AlertConditionBindings extends Graylog2Module {
    @Override
    protected void configure() {
        MapBinder<String, AlertCondition.Factory> alertConditionBinder = alertConditionBinder();
        installAlertConditionWithCustomName(alertConditionBinder,
            AbstractAlertCondition.Type.FIELD_CONTENT_VALUE.toString(),
            FieldContentValueAlertCondition.class,
            FieldContentValueAlertCondition.Factory.class);
        installAlertConditionWithCustomName(alertConditionBinder,
            AbstractAlertCondition.Type.FIELD_VALUE.toString(),
            FieldValueAlertCondition.class,
            FieldValueAlertCondition.Factory.class);
        installAlertConditionWithCustomName(alertConditionBinder,
            AbstractAlertCondition.Type.MESSAGE_COUNT.toString(),
            MessageCountAlertCondition.class,
            MessageCountAlertCondition.Factory.class);
    }
}
