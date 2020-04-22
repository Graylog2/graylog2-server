// @flow strict
import * as Immutable from 'immutable';

export type SeriesConfigJson = {
  name: string,
};

type InternalState = {
  name: ?string,
};

export default class SeriesConfig {
  _value: InternalState;

  constructor(name: ?string) {
    this._value = { name };
  }

  get name() {
    return this._value.name;
  }

  toJSON() {
    const { name } = this._value;

    return { name };
  }

  static fromJSON(value: SeriesConfigJson) {
    const { name } = value;
    return new SeriesConfig(name);
  }

  static empty() {
    return new SeriesConfig(null);
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  name(newName: string) {
    return new Builder(this.value.set('name', newName));
  }

  build() {
    const { name } = this.value.toObject();
    return new SeriesConfig(name);
  }
}
