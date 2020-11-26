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
import * as Immutable from 'immutable';

import highlightConditionFunctions from 'views/logic/views/formatting/highlighting/highlightConditionFunctions';

export type Value = string;
export type Color = string;
export type Condition = '==' | '!=' | '<=' | '>=' | '<' | '>';

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
    return this._value.condition ?? '==';
  }

  get conditionFunc() {
    return highlightConditionFunctions[this._value.condition ?? '=='];
  }

  get color() {
    return this._value.color;
  }

  toBuilder() {
    const { field, value, condition, color } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define,@typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ field, value, condition, color }));
  }

  static create(field: string, value: Value, condition: Condition, color: Color) {
    return new HighlightingRule(field, value, condition, color);
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define,@typescript-eslint/no-use-before-define
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
