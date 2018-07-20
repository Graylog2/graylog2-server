// @flow
import { Map } from 'immutable';
import uuid from 'uuid/v4';

type State = {
  id: string,
  type: string,
  config: any,
  filter: string;
};

class Widget {
  _value: State;
  constructor(id: string, type: string, config: any, filter: string) {
    this._value = { id, type, config, filter: filter === null ? undefined : filter };
  }

  static __registrations = {};

  get id() : string {
    return this._value.id;
  }

  get type() : string {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  get filter() : string {
    return this._value.filter;
  }

  duplicate(newId: string) : Widget {
    return this.toBuilder().id(newId).build();
  }

  toBuilder() : Builder {
    const { id, type, config, filter } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ id, type, config, filter }));
  }

  toJSON() {
    const { id, type, config, filter } = this._value;

    return { id, type: type.toLocaleLowerCase(), config, filter };
  }

  static fromJSON(value: State) : Widget {
    const { id, type, config, filter } = value;
    const implementingClass = Widget.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(value);
    }

    return new Widget(id, type, config, filter);
  }

  static registerSubtype(type: string, implementingClass: typeof Widget) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }
}

class Builder {
  value: Map<string, any>;
  constructor(value = Map()) {
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

  config(value) {
    this.value = this.value.set('config', value);
    return this;
  }

  filter(value: string) {
    this.value = this.value.set('filter', value);
    return this;
  }

  build() : Widget {
    const { id, type, config, filter } = this.value.toObject();
    return new Widget(id, type, config, filter);
  }
}

Widget.Builder = Builder;

export default Widget;
