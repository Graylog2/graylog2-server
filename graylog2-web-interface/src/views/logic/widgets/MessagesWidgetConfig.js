// @flow strict
import * as Immutable from 'immutable';

import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { SortConfigJson } from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';

import WidgetConfig from './WidgetConfig';

export type Decorator = {
  id: string,
  type: string,
  config: any,
  stream: ?string,
  order: number,
};

type InternalState = {
  decorators: Array<Decorator>,
  fields: Array<string>,
  sort: Array<SortConfig>,
  showMessageRow: boolean,
};

export type MessagesWidgetConfigJSON = {
  decorators: Array<Decorator>,
  fields: Array<string>,
  sort: Array<SortConfigJson>,
  show_message_row: boolean,
};

const defaultSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending)];

export default class MessagesWidgetConfig extends WidgetConfig {
  _value: InternalState;

  constructor(fields: Array<string>, showMessageRow: boolean, decorators: Array<Decorator>, sort: Array<SortConfig>) {
    super();
    this._value = { decorators, fields: fields.slice(0), showMessageRow, sort: sort && sort.length > 0 ? sort : defaultSort };
  }

  get decorators() {
    return this._value.decorators;
  }

  get fields() {
    return this._value.fields;
  }

  get sort() {
    return this._value.sort;
  }

  get showMessageRow() {
    return this._value.showMessageRow;
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map((this._value: { [string]: any })));
  }

  toJSON() {
    const { decorators, fields, showMessageRow, sort } = this._value;
    return {
      decorators,
      fields,
      show_message_row: showMessageRow,
      sort,
    };
  }

  equals(other: any): boolean {
    return other instanceof MessagesWidgetConfig && other.decorators === this.decorators && other.sort === this.sort;
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .decorators([])
      .fields([])
      .sort([]);
  }

  static fromJSON(value: MessagesWidgetConfigJSON) {
    // eslint-disable-next-line camelcase
    const { decorators, show_message_row, fields, sort } = value;

    return new MessagesWidgetConfig(fields, show_message_row, decorators, sort.map(SortConfig.fromJSON));
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

  sort(sorts: Array<SortConfig>) {
    return new Builder(this.value.set('sort', sorts));
  }

  build() {
    const { decorators, fields, showMessageRow, sort } = this.value.toObject();
    return new MessagesWidgetConfig(fields, showMessageRow, decorators, sort);
  }
}
