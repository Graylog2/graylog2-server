// @flow strict
import * as Immutable from 'immutable';
import WidgetConfig from './WidgetConfig';

type Decorator = any;

type InternalState = {
  decorators: Array<Decorator>,
  fields: Array<string>,
  showMessageRow: boolean,
};

export type MessagesWidgetConfigJSON = {
  decorators: Array<Decorator>,
  fields: Array<string>,
  show_message_row: boolean,
};

export default class MessagesWidgetConfig extends WidgetConfig {
  _value: InternalState;

  constructor(fields: Array<string>, showMessageRow: boolean, decorators: Array<Decorator>) {
    super();
    this._value = { decorators, fields: fields.slice(0), showMessageRow };
  }

  get decorators() {
    return this._value.decorators;
  }

  get fields() {
    return this._value.fields;
  }

  get showMessageRow() {
    return this._value.showMessageRow;
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  toJSON() {
    const { decorators, fields, showMessageRow } = this._value;
    return {
      decorators,
      fields,
      show_message_row: showMessageRow,
    };
  }

  // eslint-disable-next-line class-methods-use-this
  equals(other: any): boolean {
    return other instanceof MessagesWidgetConfig;
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .decorators([])
      .fields([]);
  }

  static fromJSON(value: MessagesWidgetConfigJSON) {
    // eslint-disable-next-line camelcase
    const { decorators, show_message_row, fields } = value;

    return new MessagesWidgetConfig(fields, show_message_row, decorators);
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  decorators(value: Array<Decorator>) {
    return new Builder(this.value.set('decorators', value.slice(0)));
  }

  fields(value: Array<string>) {
    return new Builder(this.value.set('fields', value.slice(0)));
  }

  showMessageRow(value: boolean) {
    return new Builder(this.value.set('showMessageRow', value));
  }

  build() {
    const { decorators, fields, showMessageRow } = this.value.toObject();
    return new MessagesWidgetConfig(fields, showMessageRow, decorators);
  }
}
