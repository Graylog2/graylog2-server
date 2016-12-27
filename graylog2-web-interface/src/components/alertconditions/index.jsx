export { default as AlertCondition } from './AlertCondition';
export { default as AlertConditionForm } from './AlertConditionForm';
export { default as AlertConditionSummary } from './AlertConditionSummary';
export { default as AlertConditionsComponent } from './AlertConditionsComponent';
export { default as AlertConditionsList } from './AlertConditionsList';
export { default as ConditionAlertNotifications } from './ConditionAlertNotifications';
export { default as CreateAlertConditionInput } from './CreateAlertConditionInput';
export { default as EditAlertConditionForm } from './EditAlertConditionForm';
export { default as GenericAlertConditionSummary } from './GenericAlertConditionSummary';
export { default as UnknownAlertCondition } from './UnknownAlertCondition';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import { FieldContentConditionForm, FieldContentConditionSummary } from 'components/alertconditions/fieldcontentcondition';
import { FieldValueConditionForm, FieldValueConditionSummary } from 'components/alertconditions/fieldvaluecondition';
import { MessageCountConditionForm, MessageCountConditionSummary } from 'components/alertconditions/messagecountcondition';

PluginStore.register(new PluginManifest({}, {
  alertConditions: [
    {
      formComponent: FieldContentConditionForm,
      summaryComponent: FieldContentConditionSummary,
      type: 'field_content_value',
    },
    {
      formComponent: FieldValueConditionForm,
      summaryComponent: FieldValueConditionSummary,
      type: 'field_value',
    },
    {
      formComponent: MessageCountConditionForm,
      summaryComponent: MessageCountConditionSummary,
      type: 'message_count',
    },
  ],
}));
