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
import { Map } from 'immutable';
import * as Immutable from 'immutable';

import type { QueryString, TimeRange } from 'views/logic/queries/Query';
import { singleton } from 'logic/singleton';
import generateId from 'logic/generateId';
import isDeepEqual from 'stores/isDeepEqual';
import type { FiltersType } from 'views/types';

export type WidgetState = {
  id: string;
  type: string;
  config: any;
  filter: string | null | undefined;
  filters?: FiltersType,
  timerange: TimeRange | null | undefined;
  query: QueryString | null | undefined;
  streams: Array<string>;
};

type DeserializesWidgets = {
  fromJSON: (value) => Widget;
};

class Widget {
  _value: WidgetState;

  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  static Builder: typeof Builder;

  constructor(id: string, type: string, config: any, filter?: string, timerange?: TimeRange, query?: QueryString, streams: Array<string> = [], filters?: FiltersType) {
    this._value = { id, type, config, filter: filter === null ? undefined : filter, filters, timerange, query, streams };
  }

  get id(): string {
    return this._value.id;
  }

  get type(): string {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  get filter(): string | null | undefined {
    return this._value.filter;
  }

  get filters(): FiltersType | null | undefined {
    return this._value.filters;
  }

  get timerange(): TimeRange | null | undefined {
    return this._value.timerange;
  }

  get query(): QueryString | null | undefined {
    return this._value.query;
  }

  get streams(): Array<string> {
    return this._value.streams;
  }

  // eslint-disable-next-line class-methods-use-this
  get isExportable() {
    return false;
  }

  equals(other: any): boolean {
    if (other === undefined) {
      return false;
    }

    if (!(other instanceof Widget)) {
      return false;
    }

    return this.id === other.id
      && this.filter === other.filter
      && isDeepEqual(this.filters, other.filters)
      && isDeepEqual(this.config, other.config)
      && isDeepEqual(this.timerange, other.timerange)
      && isDeepEqual(this.query, other.query)
      && isDeepEqual(this.streams, other.streams);
  }

  duplicate(newId: string): Widget {
    return this.toBuilder().id(newId).build();
  }

  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  toBuilder(): Builder {
    const {
      id,
      type,
      config,
      filter,
      filters,
      timerange,
      query,
      streams,
    } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Map({ id, type, config, filter, filters, timerange, query, streams }));
  }

  toJSON() {
    const {
      id,
      type,
      config,
      filter,
      filters,
      timerange,
      query,
      streams,
    } = this._value;

    return { id, type: type.toLowerCase(), config, filter, filters, timerange, query, streams };
  }

  static fromJSON(value: WidgetState): Widget {
    const {
      id,
      type,
      config,
      filter,
      filters,
      timerange,
      query,
      streams,
    } = value;
    const implementingClass = Widget.__registrations[type.toLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(value);
    }

    return new Widget(id, type, config, filter, filters, timerange, query, streams);
  }

  static empty() {
    return this.builder().build();
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }

  static __registrations: {
    [key: string]: DeserializesWidgets;
  } = {};

  static registerSubtype(type: string, implementingClass: DeserializesWidgets) {
    this.__registrations[type.toLowerCase()] = implementingClass;
  }
}

class Builder {
  value: Map<string, any>;

  constructor(value: Map<string, any> = Map()) {
    this.value = value;
  }

  id(value: string) {
    this.value = this.value.set('id', value);

    return this;
  }

  newId() {
    return this.id(generateId());
  }

  type(value: string) {
    this.value = this.value.set('type', value);

    return this;
  }

  config(value: any) {
    this.value = this.value.set('config', value);

    return this;
  }

  filter(value: string) {
    this.value = this.value.set('filter', value);

    return this;
  }

  filters(value: FiltersType | null | undefined) {
    console.log({ value });
    this.value = this.value.set('filters', value ? Immutable.List(value) : value);
    console.log(this.value);

    return this;
  }

  timerange(value: TimeRange) {
    this.value = this.value.set('timerange', value);

    return this;
  }

  query(value: QueryString) {
    this.value = this.value.set('query', value);

    return this;
  }

  streams(value: Array<string>) {
    this.value = this.value.set('streams', value);

    return this;
  }

  build(): Widget {
    const {
      id,
      type,
      config,
      filter,
      filters,
      timerange,
      query,
      streams,
    } = this.value.toObject();
    console.log('!!!!!qqqqq!!', { filters });

    return new Widget(id, type, config, filter, timerange, query, streams, filters);
  }
}

Widget.Builder = Builder;

const SingletonWidget = singleton('views.logic.widgets.Widget', () => Widget);
// eslint-disable-next-line @typescript-eslint/no-redeclare
type SingletonWidget = InstanceType<typeof Widget>;

export default SingletonWidget;
