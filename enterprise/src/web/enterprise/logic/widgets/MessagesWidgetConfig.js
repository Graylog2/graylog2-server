import Immutable from 'immutable';

export default class MessagesWidgetConfig {
  constructor(fields, showMessageRow) {
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
    return {
      fields: fields.slice(0),
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

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { show_message_row, fields } = value;

    return new MessagesWidgetConfig(fields, show_message_row);
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  fields(value) {
    return new Builder(this.value.set('fields', value.slice(0)));
  }

  showMessageRow(value) {
    return new Builder(this.value.set('showMessageRow', value));
  }

  build() {
    const { fields, showMessageRow } = this.value.toObject();
    return new MessagesWidgetConfig(fields, showMessageRow);
  }
}
