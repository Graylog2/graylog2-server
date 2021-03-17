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
import { get } from 'lodash';
import * as Immutable from 'immutable';

import SeriesConfig from './SeriesConfig';
import type { SeriesConfigJson } from './SeriesConfig';

export type SeriesJson = {
  config: SeriesConfigJson,
  function: string,
};

type InternalState = {
  config: SeriesConfig,
  function: string,
};

export type Definition = {
  type: string,
  field?: string,
  percentile?: string,
};

const parametersRegex = /\((.+)\)/;
const funcNameRegex = /(\w+)\(/;
const testSeriesRegex = /^(\w+)\((.*)(,(\w+))*\)$/;

const definitionFor = (type: string, parameters: Array<string>): Definition => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [field, parameter] = parameters;

  if (type === 'percentile') {
    return { type, field, percentile: parameter };
  }

  return { type, field };
};

export const isFunction = (s: string) => testSeriesRegex.test(s);

export const parseSeries = (s: string) => {
  const funcNameResult = funcNameRegex.exec(s);

  if (!funcNameResult) {
    return null;
  }

  const type = funcNameResult[1];
  const definition: Definition = {
    type,
  };

  const parameterResult = parametersRegex.exec(s);

  if (!parameterResult) {
    return definition;
  }

  return definitionFor(type, parameterResult[1].split(','));
};

export default class Series {
  private readonly _value: InternalState;

  constructor(func: string, config: SeriesConfig = SeriesConfig.empty()) {
    this._value = { function: func, config };
  }

  get function() {
    return this._value.function;
  }

  get config() {
    return this._value.config;
  }

  get effectiveName(): string {
    const overridenName = get(this, 'config.name');

    return overridenName || this.function;
  }

  toString() {
    return `Series: ${this.effectiveName}, config={${JSON.stringify(this.config)}}`;
  }

  toJSON() {
    return {
      config: this._value.config,
      function: this._value.function,
    };
  }

  static fromJSON(value: SeriesJson) {
    return new Series(value.function, SeriesConfig.fromJSON(value.config));
  }

  static forFunction(func: string) {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .function(func)
      .config(SeriesConfig.empty())
      .build();
  }

  static create(func: string, field?: string, parameter?: string) {
    const optionalParameter = parameter ? `,${parameter}` : '';
    const functionWithField = `${func}(${field ?? ''}${optionalParameter})`;

    return Series.forFunction(functionWithField);
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  private readonly value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  function(newFunction: string) {
    return new Builder(this.value.set('function', newFunction));
  }

  config(newConfig: SeriesConfig) {
    return new Builder(this.value.set('config', newConfig));
  }

  parameter(newParameter: any) {
    return new Builder(this.value.set('parameter', newParameter));
  }

  build() {
    const { config } = this.value.toObject();
    const func = this.value.get('function');

    return new Series(func, config);
  }
}
