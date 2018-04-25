import Immutable from 'immutable';

export default class Widget {
  constructor(id, type, config) {
    this._value = { id, type, config };
  }

  get id() {
    return this._value.id;
  }

  get type() {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  duplicate(newId) {
    return new Widget(newId, this.type, this.config);
  }

  toBuilder() {
    const { id, type, config } = this._value;
    return new Builder(Immutable.Map({ id, type, config }));
  }

  toJSON() {
    const { id, type, config } = this._value;

    return { id, type: type.toLocaleLowerCase(), config };
  }

  static fromJSON(value) {
    const { id, type, config } = value;
    return new Widget(id, type, Object.assign({}, config));
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  id(value) {
    return new Builder(this.value.set('id', value));
  }

  type(value) {
    return new Builder(this.value.set('type', value));
  }

  config(value) {
    return new Builder(this.value.set('config', value));
  }

  build() {
    const { id, type, config } = this.value.toObject();
    return new Widget(id, type, config);
  }
}
