import { get } from 'lodash';
import * as Immutable from 'immutable';

import SeriesConfig from './SeriesConfig';

export default class Series {
  constructor(func, config = SeriesConfig.empty()) {
    this._value = { function: func, config };
  }

  get function() {
    return this._value.function;
  }

  get config() {
    return this._value.config;
  }

  get effectiveName() {
    const overridenName = get(this, 'config.name');
    return overridenName || this.function;
  }

  toString() {
    return `Series: ${this.effectiveName}, config={${this.config.toJSON()}}`;
  }

  toJSON() {
    return {
      config: this._value.config,
      function: this._value.function,
    };
  }

  static fromJSON(value) {
    return new Series(value.function, SeriesConfig.fromJSON(value.config));
  }

  static forFunction(func) {
    // eslint-disable-next-line no-use-before-define
    return new Builder().function(func).config(SeriesConfig.empty()).build();
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  function(newFunction) {
    return new Builder(this.value.set('function', newFunction));
  }

  config(newConfig) {
    return new Builder(this.value.set('config', newConfig));
  }

  build() {
    const { config } = this.value.toObject();
    const func = this.value.get('function');
    return new Series(func, config);
  }
}
