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
import findIndex from 'lodash/findIndex';

import ValueRefHelper from 'util/ValueRefHelper';

import Constraint from './Constraint';

type ConstraintJSON = {
  type: string;
  version: string;
  plugin?: string;
};

type EntityParameter = {
  name: string;
  default_value?: string;
};

type EntityData = Record<string, unknown>;

type EntityValue = {
  v: string;
  type: string;
  id: string;
  data: EntityData;
  constraints: Array<Constraint>;
  fromServer: boolean;
  parameters: Array<EntityParameter>;
};

type EntityJSON = {
  v: string;
  type: string;
  id: string;
  data: EntityData;
  constraints: Array<ConstraintJSON | Constraint>;
};

export default class Entity {
  _value: EntityValue;

  constructor(v: string, type: string, id: string, data: EntityData, fromServer: boolean = false, constraintValues: Array<ConstraintJSON | Constraint> = [], parameters: Array<EntityParameter> = []) {
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

  static fromJSON(value: EntityJSON, fromServer: boolean = true, parameters: Array<EntityParameter> = []): Entity {
    const { v, type, id, data, constraints } = value;

    return new Entity(v, type, id, data, fromServer, constraints, parameters);
  }

  get v(): string {
    return this._value.v;
  }

  get type(): any {
    return this._value.type;
  }

  get id(): string {
    return this._value.id;
  }

  get data(): EntityData {
    return this._value.data;
  }

  get fromServer(): boolean {
    return this._value.fromServer;
  }

  get constraints(): Array<Constraint> {
    return this._value.constraints;
  }

  get title(): string {
    let value = this.getValueFromData('title');

    if (!value) {
      value = this.getValueFromData('name');
    }

    return value || '';
  }

  get description(): string {
    return this.getValueFromData('description') || '';
  }

  /* eslint-disable-next-line class-methods-use-this */
  get isEntity(): boolean {
    return true;
  }

  /* implement custom instanceof */
  static [Symbol.hasInstance](obj: unknown): boolean {
    return !!(obj as { isEntity?: boolean })?.isEntity;
  }

  getValueFromData(key: string): string | undefined {
    const { data } = this._value;

    if (!data || !data[key]) {
      return undefined;
    }

    if (ValueRefHelper.dataIsValueRef(data[key])) {
      const value = ((data[key] || {}) as Record<string, string>)[ValueRefHelper.VALUE_REF_VALUE_FIELD];

      if (ValueRefHelper.dataValueIsParameter(data[key])) {
        const index = findIndex(this._value.parameters, { name: value });

        if (index >= 0 && this._value.parameters[index].default_value) {
          return this._value.parameters[index].default_value;
        }
      }

      return value;
    }

    return data[key] as string;
  }

  toBuilder(): Builder {
    const { v, type, id, data, constraints, fromServer, parameters } = this._value;

    /* eslint-disable-next-line no-use-before-define */
    return new Builder(
      Map({
        v,
        type,
        id,
        data,
        constraints,
        fromServer,
        parameters,
      }),
    );
  }

  static builder(): Builder {
    /* eslint-disable-next-line no-use-before-define */
    return new Builder();
  }

  toJSON(): Omit<EntityJSON, 'constraints'> & { constraints: Array<Constraint> } {
    const { v, type, id, data, constraints } = this._value;

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
  value: Map<string, unknown>;

  constructor(value: Map<string, unknown> = Map()) {
    this.value = value;
  }

  v(value: any): Builder {
    this.value = this.value.set('v', value);

    return this;
  }

  type(value: any): Builder {
    this.value = this.value.set('type', value);

    return this;
  }

  id(value: string): Builder {
    this.value = this.value.set('id', value);

    return this;
  }

  data(value: EntityData): Builder {
    this.value = this.value.set('data', value);

    return this;
  }

  fromServer(value: boolean): Builder {
    this.value = this.value.set('fromServer', value);

    return this;
  }

  constraints(value: Array<Constraint>): Builder {
    this.value = this.value.set('constraints', value);

    return this;
  }

  parameters(value: Array<EntityParameter>): Builder {
    this.value = this.value.set('parameters', value);

    return this;
  }

  build(): Entity {
    const { v, type, id, data, constraints, fromServer, parameters } = this.value.toObject() as EntityValue;

    return new Entity(v, type, id, data, fromServer, constraints, parameters);
  }
}
