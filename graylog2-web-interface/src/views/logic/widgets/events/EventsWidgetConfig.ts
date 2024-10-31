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

import WidgetConfig from 'views/logic/widgets/WidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';

import type { EventsWidgetSortConfigJSON } from './EventsWidgetSortConfig';
import EventsWidgetSortConfig from './EventsWidgetSortConfig';

export const LIST_MODE = 'List';
export const NUMBER_MODE = 'Number';

export type VisualizationType = typeof LIST_MODE | typeof NUMBER_MODE;

export type Filter = {
  field: string,
  value: Array<string>,
}

type InternalState = {
  fields: Immutable.OrderedSet<string>,
  filters: Immutable.OrderedSet<Filter>,
  sort: EventsWidgetSortConfig,
  mode: VisualizationType,
};

export type EventsWidgetConfigJSON = {
  fields: Array<string>,
  filters: Array<Filter>,
  sort: EventsWidgetSortConfigJSON,
  mode: VisualizationType,
};

export default class EventsWidgetConfig extends WidgetConfig {
  _value: InternalState;

  static defaultFields: InternalState['fields'] = Immutable.OrderedSet(['message', 'event_definition_id', 'alert', 'timestamp']);

  static defaultSort: InternalState['sort'] = new EventsWidgetSortConfig('timestamp', Direction.Descending);

  static defaultMode: InternalState['mode'] = LIST_MODE;

  constructor(
    fields: InternalState['fields'],
    filters: InternalState['filters'],
    sort: InternalState['sort'],
    mode: InternalState['mode'],
  ) {
    super();

    this._value = { fields, filters, sort, mode };
  }

  get fields() {
    return this._value.fields;
  }

  get filters() {
    return this._value.filters;
  }

  get sort() {
    return this._value.sort;
  }

  get mode() {
    return this._value.mode;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map((this._value)));
  }

  toJSON() {
    const {
      fields = Immutable.OrderedSet(),
      filters = Immutable.OrderedSet(),
      sort,
      mode,
    } = this._value;

    return {
      fields: fields.toArray(),
      filters: filters.toArray(),
      sort: sort.toJSON(),
      mode,
    };
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .fields(Immutable.OrderedSet())
      .filters(Immutable.OrderedSet());
  }

  static fromJSON(value: EventsWidgetConfigJSON) {
    const { fields, filters, sort, mode } = value;

    return new EventsWidgetConfig(Immutable.OrderedSet(fields), Immutable.OrderedSet(filters), EventsWidgetSortConfig.fromJSON(sort), mode);
  }

  static createDefault() {
    return new EventsWidgetConfig(
      EventsWidgetConfig.defaultFields,
      Immutable.OrderedSet(),
      EventsWidgetConfig.defaultSort,
      EventsWidgetConfig.defaultMode,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  fields(value: InternalState['fields']) {
    return new Builder(this.value.set('fields', value));
  }

  filters(value: InternalState['filters']) {
    return new Builder(this.value.set('filters', value));
  }

  sort(value: InternalState['sort']) {
    return new Builder(this.value.set('sort', value));
  }

  mode(value: InternalState['mode']) {
    return new Builder(this.value.set('mode', value));
  }

  build() {
    const { fields, filters, sort, mode } = this.value.toObject();

    return new EventsWidgetConfig(fields, filters, sort, mode);
  }
}
