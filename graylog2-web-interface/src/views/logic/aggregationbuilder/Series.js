// @flow strict
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
const testSeriesRegex = /^(\w+)\((\w*)(,(\w+))*\)$/;

const definitionFor = (type: string, parameters: Array<string>): Definition => {
  // eslint-disable-next-line no-unused-vars
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
  _value: InternalState;

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
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .function(func)
      .config(SeriesConfig.empty())
      .build();
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  function(newFunction: string) {
    return new Builder(this.value.set('function', newFunction));
  }

  config(newConfig: SeriesConfig) {
    return new Builder(this.value.set('config', newConfig));
  }

  parameter(newParameter: *) {
    return new Builder(this.value.set('parameter', newParameter));
  }

  build() {
    const { config } = this.value.toObject();
    const func = this.value.get('function');
    return new Series(func, config);
  }
}
