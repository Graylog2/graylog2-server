// @flow strict
import * as Immutable from 'immutable';
import WidgetConfig from './WidgetConfig';

type InternalState = {
  fields: Array<string>,
  showMessageRow: boolean,
};

export type MessagesWidgetConfigJSON = {
  fields: Array<string>,
  show_message_row: boolean,
};

export default class MessagesWidgetConfig extends WidgetConfig {
  _value: InternalState;

  constructor(fields: Array<string>, showMessageRow: boolean) {
    super();
    this._value = { fields: fields.slice(0), showMessageRow };
  }

  get fields() {
    return this._value.fields;
  }

  get showMessageRow() {
    return this._value.showMessageRow;
  }

  toObject() {
    const { fields, showMessageRow } = this._value;
    const copiedFields: Array<string> = fields.slice(0);
    return {
      fields: copiedFields,
      showMessageRow,
    };
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  toJSON() {
    const { fields, showMessageRow } = this._value;
    return {
      fields,
      show_message_row: showMessageRow,
    };
  }

  // eslint-disable-next-line class-methods-use-this
  equals(other: any) : boolean {
    return other instanceof MessagesWidgetConfig;
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .fields([]);
  }

  static fromJSON(value: MessagesWidgetConfigJSON) {
    // eslint-disable-next-line camelcase
    const { show_message_row, fields } = value;

    return new MessagesWidgetConfig(fields, show_message_row);
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  fields(value: Array<string>) {
    return new Builder(this.value.set('fields', value.slice(0)));
  }

  showMessageRow(value: boolean) {
    return new Builder(this.value.set('showMessageRow', value));
  }

  build() {
    const { fields, showMessageRow } = this.value.toObject();
    return new MessagesWidgetConfig(fields, showMessageRow);
  }
}
