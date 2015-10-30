import MessageCountDefinition from 'components/alertconditions/messagecountcondition';
import FieldValueDefinition from 'components/alertconditions/fieldvaluecondition';
import FieldContentDefinition from 'components/alertconditions/fieldcontentcondition';

export default class AlertConditionsFactory {
  constructor() {
    this.alertConditions = {
      message_count: MessageCountDefinition,
      field_value: FieldValueDefinition,
      field_content_value: FieldContentDefinition,
    };
  }

  get(type) {
    return this.alertConditions[type];
  }

  available() {
    return this.alertConditions;
  }
}
