import * as Immutable from 'immutable';

export default class SeriesConfig {
  constructor(name) {
    this._value = { name };
  }

  get name() {
    return this._value.name;
  }

  toJSON() {
    const { name } = this._value;

    return { name };
  }

  static fromJSON(value) {
    const { name } = value;
    return new SeriesConfig(name);
  }

  static empty() {
    return new SeriesConfig(null, null);
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  name(newName) {
    return new Builder(this.value.set('name', newName));
  }

  build() {
    const { name } = this.value.toObject();
    return new SeriesConfig(name);
  }
}
