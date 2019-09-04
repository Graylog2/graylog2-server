// @flow strict

import Parameter from 'views/logic/parameters/Parameter';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import * as Immutable from 'immutable';
import type { ParameterJson } from 'views/logic/parameters/Parameter';

type InternalBuilderState = Immutable.Map<string, any>;

export default class ValueParameter extends Parameter {
  static type = 'value-parameter-v1';

  // eslint-disable-next-line no-use-before-define
  static Builder: typeof Builder;

  constructor(name: string, title: string, description: string, dataType: string, defaultValue: any, optional: boolean, binding: ?ParameterBinding) {
    super(ValueParameter.type, name, title, description, dataType, defaultValue, optional, binding);
  }

  static create(name: string, title: string, description: string, dataType: string, defaultValue: any, optional: boolean, binding: ?ParameterBinding): ValueParameter {
    return new ValueParameter(name, title, description, dataType, defaultValue, optional, binding);
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { type, name, title, description, dataType, defaultValue, optional, binding } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ type, name, title, description, dataType, defaultValue, optional, binding }));
  }

  toJSON(): ParameterJson {
    const { type, name, title, description, dataType, defaultValue, optional, binding } = this._value;

    return {
      type,
      name,
      title,
      description,
      data_type: dataType,
      default_value: defaultValue,
      optional,
      binding: binding ? binding.toJSON() : undefined,
    };
  }

  static fromJSON(value: ParameterJson): ValueParameter {
    // eslint-disable-next-line camelcase
    const { name, title, description, data_type, default_value, optional, binding } = value;
    return new ValueParameter(name, title, description, data_type, default_value, optional, ParameterBinding.fromJSON(binding));
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .type(ValueParameter.type)
      .optional(false)
      .dataType('any')
      .binding(ParameterBinding.empty());
  }
}

class Builder {
  value: InternalBuilderState;

  constructor(value: Immutable.Map = Immutable.Map()) {
    this.value = value;
  }

  type(value: string): Builder {
    return new this.constructor(this.value.set('type', value));
  }

  name(value: string): Builder {
    return new this.constructor(this.value.set('name', value));
  }

  title(value: string): Builder {
    return new this.constructor(this.value.set('title', value));
  }

  description(value: string): Builder {
    return new this.constructor(this.value.set('description', value));
  }

  dataType(value: string): Builder {
    return new this.constructor(this.value.set('dataType', value));
  }

  defaultValue(value: any): Builder {
    return new this.constructor(this.value.set('defaultValue', value));
  }

  optional(value: boolean): Builder {
    return new this.constructor(this.value.set('optional', value));
  }

  binding(value: ParameterBinding): Builder {
    return new this.constructor(this.value.set('binding', value));
  }

  build(): ValueParameter {
    const { name, title, description, dataType, defaultValue, optional, binding } = this.value.toObject();
    return new ValueParameter(name, title, description, dataType, defaultValue, optional, binding);
  }
}

ValueParameter.Builder = Builder;
