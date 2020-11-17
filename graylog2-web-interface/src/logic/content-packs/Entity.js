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
import { findIndex } from 'lodash';

import ValueRefHelper from 'util/ValueRefHelper';

import Constraint from './Constraint';

export default class Entity {
  constructor(v, type, id, data, fromServer = false, constraintValues = [], parameters = []) {
    const constraints = constraintValues.map((c) => {
      if (c instanceof Constraint) {
        return c;
      }

      return Constraint.fromJSON(c);
    });

    this._value = {
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
      parameters,
    };
  }

  static fromJSON(value, fromServer = true, parameters = []) {
    const { v, type, id, data, constraints } = value;

    return new Entity(v, type, id, data, fromServer, constraints, parameters);
  }

  get v() {
    return this._value.v;
  }

  get type() {
    return this._value.type;
  }

  get id() {
    return this._value.id;
  }

  get data() {
    return this._value.data;
  }

  get fromServer() {
    return this._value.fromServer;
  }

  get constraints() {
    return this._value.constraints;
  }

  get title() {
    let value = this.getValueFromData('title');

    if (!value) {
      value = this.getValueFromData('name');
    }

    return value || '';
  }

  get description() {
    return this.getValueFromData('description') || '';
  }

  /* eslint-disable-next-line class-methods-use-this */
  get isEntity() {
    return true;
  }

  /* implement custom instanceof */
  static [Symbol.hasInstance](obj) {
    if (obj.isEntity) {
      return true;
    }

    return false;
  }

  getValueFromData(key) {
    const { data } = this._value;

    if (!data || !data[key]) {
      return undefined;
    }

    if (ValueRefHelper.dataIsValueRef(data[key])) {
      const value = (data[key] || {})[ValueRefHelper.VALUE_REF_VALUE_FIELD];

      if (ValueRefHelper.dataValueIsParameter(data[key])) {
        const index = findIndex(this._value.parameters, { name: value });

        if (index >= 0 && this._value.parameters[index].default_value) {
          return this._value.parameters[index].default_value;
        }
      }

      return value;
    }

    return data[key];
  }

  toBuilder() {
    const {
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
      parameters,
    } = this._value;

    /* eslint-disable-next-line no-use-before-define */
    return new Builder(Map({
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
      parameters,
    }));
  }

  static builder() {
    /* eslint-disable-next-line no-use-before-define */
    return new Builder();
  }

  toJSON() {
    const {
      v,
      type,
      id,
      data,
      constraints,
    } = this._value;

    return {
      v,
      type,
      id,
      data,
      constraints,
    };
  }
}

class Builder {
  constructor(value = Map()) {
    this.value = value;
  }

  v(value) {
    this.value = this.value.set('v', value);

    return this;
  }

  type(value) {
    this.value = this.value.set('type', value);

    return this;
  }

  id(value) {
    this.value = this.value.set('id', value);

    return this;
  }

  data(value) {
    this.value = this.value.set('data', value);

    return this;
  }

  fromServer(value) {
    this.value = this.value.set('fromServer', value);

    return this;
  }

  constraints(value) {
    this.value = this.value.set('constraints', value);

    return this;
  }

  parameters(value) {
    this.value = this.value.set('parameters', value);

    return this;
  }

  build() {
    const {
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
      parameters,
    } = this.value.toObject();

    return new Entity(v, type, id, data, fromServer, constraints, parameters);
  }
}
