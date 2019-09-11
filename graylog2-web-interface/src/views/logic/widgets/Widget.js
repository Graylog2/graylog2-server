// @flow strict
import { Map } from 'immutable';
import uuid from 'uuid/v4';

import type { QueryString, TimeRange } from 'views/logic/queries/Query';

type State = {
  id: string,
  type: string,
  config: any,
  filter: ?string;
  timerange: ?TimeRange,
  query: ?QueryString,
};

class Widget {
  _value: State;

  // eslint-disable-next-line no-use-before-define
  static Builder: typeof Builder;

  constructor(id: string, type: string, config: any, filter: ?string, timerange: ?TimeRange, query: ?QueryString) {
    this._value = { id, type, config, filter: filter === null ? undefined : filter, timerange, query };
  }

  get id(): string {
    return this._value.id;
  }

  get type(): string {
    return this._value.type;
  }

  get config(): any {
    return this._value.config;
  }

  get filter(): ?string {
    return this._value.filter;
  }

  get timerange(): ?TimeRange {
    return this._value.timerange;
  }

  get query(): ?QueryString {
    return this._value.query;
  }

  duplicate(newId: string): Widget {
    return this.toBuilder().id(newId).build();
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, type, config, filter, timerange, query } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ id, type, config, filter, timerange, query }));
  }

  toJSON() {
    const { id, type, config, filter, timerange, query } = this._value;

    return { id, type: type.toLocaleLowerCase(), config, filter, timerange, query };
  }

  static fromJSON(value: State): Widget {
    const { id, type, config, filter, timerange, query } = value;
    const implementingClass = Widget.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(value);
    }

    return new Widget(id, type, config, filter, timerange, query);
  }

  static empty() {
    return this.builder().build();
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }

  static __registrations: { [string]: typeof Widget } = {};

  static registerSubtype(type: string, implementingClass: typeof Widget) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
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
    return this.id(uuid());
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

  timerange(value: TimeRange) {
    this.value = this.value.set('timerange', value);
    return this;
  }

  query(value: QueryString) {
    this.value = this.value.set('query', value);
    return this;
  }

  build(): Widget {
    const { id, type, config, filter, timerange, query } = this.value.toObject();
    return new Widget(id, type, config, filter, timerange, query);
  }
}

Widget.Builder = Builder;

export default Widget;
