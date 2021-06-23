/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as Immutable from 'immutable';

import isDeepEqual from 'stores/isDeepEqual';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { SortConfigJson } from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import { TIMESTAMP_FIELD } from 'views/Constants';

import WidgetConfig from './WidgetConfig';

export type Decorator = {
  id: string,
  type: string,
  config: any,
  stream: string | undefined | null,
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

export const defaultSortDirection = Direction.Descending;
export const defaultSort = [new SortConfig(SortConfig.PIVOT_TYPE, TIMESTAMP_FIELD, defaultSortDirection)];

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
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map((this._value)));
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
    return other instanceof MessagesWidgetConfig
      && isDeepEqual(this.decorators, other.decorators)
      && isDeepEqual(this.fields, other.fields)
      && isDeepEqual(this.sort, other.sort)
      && this.showMessageRow === other.showMessageRow;
  }

  equalsForSearch(other: any): boolean {
    return other instanceof MessagesWidgetConfig
      && isEqualForSearch(other.decorators, this.decorators)
      && isEqualForSearch(other.sort, this.sort);
  }

  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .decorators([])
      .fields([])
      .sort([]);
  }

  static fromJSON(value: MessagesWidgetConfigJSON) {
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
