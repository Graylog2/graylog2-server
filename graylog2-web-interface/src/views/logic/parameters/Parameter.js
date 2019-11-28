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
  binding: ?ParameterBinding
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
