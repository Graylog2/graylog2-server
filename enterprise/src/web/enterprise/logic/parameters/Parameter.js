// @flow strict

import * as Immutable from 'immutable';
import ParameterBinding from './ParameterBinding';

type InternalState = {
  name: string,
  title: string,
  description: string,
  dataType: string,
  defaultValue: any,
  optional: boolean,
  binding: ParameterBinding,
};

type InternalBuilderState = Immutable.Map<string, any>;

export type ParameterJson = {
  name: string,
  title: string,
  description: string,
  data_type: string,
  default_value: any,
  optional: boolean,
  binding: ParameterBinding,
};

export default class Parameter {
  _value: InternalState;

  constructor(name: string, title: string, description: string, dataType: string, defaultValue: any, optional: boolean, binding: ParameterBinding) {
    this._value = { name, title, description, dataType, defaultValue, optional, binding };
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

  get binding(): ParameterBinding {
    return this._value.binding;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { name, title, description, dataType, defaultValue, optional, binding } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ name, title, description, dataType, defaultValue, optional, binding }));
  }

  static create(name: string, title: string, description: string, dataType: string, defaultValue: any, optional: boolean, binding: ParameterBinding): Parameter {
    return new Parameter(name, title, description, dataType, defaultValue, optional, binding);
  }

  toJSON(): ParameterJson {
    const { name, title, description, dataType, defaultValue, optional, binding } = this._value;

    return {
      name,
      title,
      description,
      data_type: dataType,
      default_value: defaultValue,
      optional,
      binding,
    };
  }

  static fromJSON(value: ParameterJson): Parameter {
    // eslint-disable-next-line camelcase
    const { name, title, description, data_type, default_value, optional, binding } = value;
    return Parameter.create(name, title, description, data_type, default_value, optional, binding);
  }

  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .optional(false)
      .dataType('any')
      .binding(ParameterBinding.empty());
  }
}

class Builder {
  value: InternalBuilderState;

  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  name(value: string): Builder {
    return new Builder(this.value.set('name', value));
  }

  title(value: string): Builder {
    return new Builder(this.value.set('title', value));
  }

  description(value: string): Builder {
    return new Builder(this.value.set('description', value));
  }

  dataType(value: string): Builder {
    return new Builder(this.value.set('dataType', value));
  }

  defaultValue(value: any): Builder {
    return new Builder(this.value.set('defaultValue', value));
  }

  optional(value: boolean): Builder {
    return new Builder(this.value.set('optional', value));
  }

  binding(value: ParameterBinding): Builder {
    return new Builder(this.value.set('binding', value));
  }

  build(): Parameter {
    const { name, title, description, dataType, defaultValue, optional, binding } = this.value.toObject();
    return new Parameter(name, title, description, dataType, defaultValue, optional, binding);
  }
}

export type ParameterMap = Immutable.Map<string, Parameter>;
