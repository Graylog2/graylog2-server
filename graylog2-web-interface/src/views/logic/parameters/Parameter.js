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
// @flow strict

import * as Immutable from 'immutable';

import ParameterBinding from 'views/logic/parameters/ParameterBinding';

import type { ParameterBindingJsonRepresentation } from './ParameterBinding';

import { singleton } from '../singleton';

type InternalState = {
  type: string,
  name: string,
  title: string,
  description: string,
  dataType: string,
  defaultValue: any,
  optional: boolean,
  binding: ?ParameterBinding,
};

export type ParameterJson = {
  type: string,
  name: string,
  title: string,
  description: string,
  data_type: string,
  default_value: any,
  optional: boolean,
  binding: ?ParameterBindingJsonRepresentation,
};

class Parameter {
  _value: InternalState;

  static __registrations: { [string]: typeof Parameter } = {};

  constructor(type: string, name: string, title: string, description: string, dataType: string, defaultValue: any, optional: boolean, binding: ?ParameterBinding) {
    this._value = { type, name, title, description, dataType, defaultValue, optional, binding };
  }

  get type(): string {
    return this._value.type;
  }

  get name(): string {
    return this._value.name;
  }

  get title(): string {
    return this._value.title;
  }

  get description(): string {
    return this._value.description;
  }

  get dataType(): string {
    return this._value.dataType;
  }

  get defaultValue(): any {
    return this._value.defaultValue;
  }

  get optional(): boolean {
    return this._value.optional;
  }

  // screw you eslint, using param.constructor.needsBinding() is ugly
  // eslint-disable-next-line class-methods-use-this
  get needsBinding(): boolean {
    return true;
  }

  get binding(): ?ParameterBinding {
    return this._value.binding;
  }

  static fromJSON(value: ParameterJson): Parameter {
    const { type = 'value-parameter-v1' } = value; // default to ValueParameter in case type is empty
    const implementingClass = Parameter.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(value);
    }

    throw new Error(`No class found for type <${type}>`);
  }

  static registerSubtype(type: string, implementingClass: typeof Parameter) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }
}

export default singleton('views.logic.parameters.Parameter', () => Parameter);

export type ParameterMap = Immutable.Map<string, Parameter>;
