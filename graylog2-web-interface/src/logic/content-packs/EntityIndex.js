import Immutable from 'immutable';

export default class EntityIndex {
  constructor(id, title, type) {
    this._value = { id, title, type };
  }

  get id() {
    return this._value.id;
  }

  get type() {
    return this._value.type;
  }

  get title() {
    return this._value.title;
  }

  toBuilder() {
    const { id, title, type } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, title, type }));
  }

  static create(id, title, type) {
    return new EntityIndex(id, title, type);
  }

  toJSON() {
    const { id, type } = this._value;

    return {
      id,
      type,
    };
  }

  /* eslint-disable-next-line class-methods-use-this */
  get isEntityIndex() {
    return true;
  }

  /* implement custom instanceof */
  static [Symbol.hasInstance](obj) {
    if (obj.isEntityIndex) {
      return true;
    }

    return false;
  }

  static fromJSON(value) {
    const { id, title, type } = value;

    return EntityIndex.create(id, title, type);
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  id(value) {
    return new Builder(this.value.set('id', value));
  }

  title(value) {
    return new Builder(this.value.set('title', value));
  }

  type(value) {
    return new Builder(this.value.set('type', value));
  }

  build() {
    const { id, title, type } = this.value.toObject();

    return new EntityIndex(id, title, type);
  }
}
