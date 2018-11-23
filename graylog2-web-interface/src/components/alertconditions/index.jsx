import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import { FieldContentConditionSummary } from 'components/alertconditions/fieldcontentcondition';
import { FieldValueConditionSummary } from 'components/alertconditions/fieldvaluecondition';
import { MessageCountConditionSummary } from 'components/alertconditions/messagecountcondition';

export { default as AlertCondition } from './AlertCondition';
export { default as AlertConditionForm } from './AlertConditionForm';
export { default as AlertConditionSummary } from './AlertConditionSummary';
export { default as AlertConditionsComponent } from './AlertConditionsComponent';
export { default as AlertConditionsList } from './AlertConditionsList';
export { default as CreateAlertConditionInput } from './CreateAlertConditionInput';
export { default as EditAlertConditionForm } from './EditAlertConditionForm';
export { default as GenericAlertConditionSummary } from './GenericAlertConditionSummary';
export { default as StreamAlertConditions } from './StreamAlertConditions';
export { default as UnknownAlertCondition } from './UnknownAlertCondition';

PluginStore.register(new PluginManifest({}, {
  alertConditions: [
    {
      summaryComponent: FieldContentConditionSummary,
      type: 'field_content_value',
    },
    {
      summaryComponent: FieldValueConditionSummary,
      type: 'field_value',
    },
    {
      summaryComponent: MessageCountConditionSummary,
      type: 'message_count',
    },
  ],
}));
