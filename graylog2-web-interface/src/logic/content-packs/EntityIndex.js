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
