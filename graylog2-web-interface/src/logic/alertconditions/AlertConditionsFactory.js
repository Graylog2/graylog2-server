import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import FieldContentDefinition from 'components/alertconditions/fieldcontentcondition';
import FieldValueDefinition from 'components/alertconditions/fieldvaluecondition';
import MessageCountDefinition from 'components/alertconditions/messagecountcondition';

PluginStore.register(new PluginManifest({}, {
  alertConditions: {
    field_content_value: FieldContentDefinition,
    field_value: FieldValueDefinition,
    message_count: MessageCountDefinition,
  },
}));

export default class AlertConditionsFactory {
  constructor() {
    this.alertConditions = PluginStore.exports('alertConditions');
  }

  get(type) {
    return [].concat(this.alertConditions.map(entry => entry[type]).filter(entry => entry !== undefined));
  }

  available() {
    return this.alertConditions;
  }
}
