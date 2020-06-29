// @flow strict
import * as Immutable from 'immutable';

export type Value = string;
export type Color = string;
export type Condition = any;

export type HighlightingRuleJSON = {
  field: string,
  value: Value,
  condition: Condition,
  color: Color,
};

type InternalState = {
  field: string,
  value: Value,
  condition: Condition,
  color: Color,
};

export default class HighlightingRule {
  _value: InternalState;

  constructor(field: string, value: Value, condition: Condition, color: Color) {
    this._value = { field, value, condition, color };
  }

  get field() {
    return this._value.field;
  }

  get value() {
    return this._value.value;
  }

  get condition() {
    return this._value.condition;
  }

  get color() {
    return this._value.color;
  }

  toBuilder() {
    const { field, value, condition, color } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ field, value, condition, color }));
  }

  static create(field: string, value: Value, condition: Condition, color: Color) {
    return new HighlightingRule(field, value, condition, color);
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }

  toJSON() {
    const { field, value, condition, color } = this._value;

    return {
      field,
      value,
      condition,
      color,
    };
  }

  static fromJSON(json: HighlightingRuleJSON) {
    const { field, value, condition, color } = json;

    return HighlightingRule.create(field, value, condition, color);
  }
}

class Builder {
  _value: Immutable.Map<string, any>;

  constructor(value: Immutable.Map<string, any> = Immutable.Map()) {
    this._value = value;
  }

  field(field: string) {
    return new Builder(this._value.set('field', field));
  }

  value(value: Value) {
    return new Builder(this._value.set('value', value));
  }

  condition(condition: Condition) {
    return new Builder(this._value.set('condition', condition));
  }

  color(color: Color) {
    return new Builder(this._value.set('color', color));
  }

  build() {
    const { field, value, condition, color } = this._value.toObject();

    return new HighlightingRule(field, value, condition, color);
  }
}
