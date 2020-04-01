// @flow strict

type ConfigType = { [string]: mixed };

export type PivotJson = {
  field: string,
  type: string,
  config: ConfigType,
};

type InternalState = {
  field: string,
  type: string,
  config: ConfigType,
};

export default class Pivot {
  _value: InternalState;

  constructor(field: string, type: string, config: ConfigType = {}) {
    this._value = { field, type, config };
  }

  get field() {
    return this._value.field;
  }

  get type() {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  static create(field: string, type: string, config: ConfigType = {}) {
    return new Pivot(field, type, config);
  }

  static fromJSON(value: PivotJson) {
    const { field, type, config = {} } = value;
    return new Pivot(field, type, config);
  }

  toJSON(): PivotJson {
    const { field, type, config } = this._value;
    return { field, type, config };
  }
}
