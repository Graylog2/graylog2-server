// @flow strict
import Immutable from 'immutable';

import VisualizationConfig from './VisualizationConfig';

export type BarMode = "stack" | "group" | "overlay" | "relative";

export type BarVisualizationConfigType = {
  barmode: BarMode,
};

export default class BarVisualizationConfig extends VisualizationConfig {
  _value: BarVisualizationConfigType;

  constructor(barmode: BarMode) {
    super();
    this._value = { barmode };
  }

  get barmode() {
    return this._value.barmode;
  }

  get opacity() {
    return this.barmode === 'overlay' ? 0.75 : 1.0;
  }

  toBuilder() {
    const { barmode } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ barmode }));
  }

  static create(barmode: BarMode) {
    return new BarVisualizationConfig(barmode);
  }

  toJSON() {
    const { barmode } = this._value;

    return {
      barmode,
    };
  }

  static fromJSON(type: string, value: BarVisualizationConfigType) {
    const { barmode } = value;
    return BarVisualizationConfig.create(barmode);
  }
}

class Builder {
  value: Immutable.Map<BarVisualizationConfigType>;

  constructor(value: BarVisualizationConfigType = Immutable.Map()) {
    this.value = value;
  }

  barmode(value: BarMode) {
    return new Builder(this.value.set('barmode', value));
  }

  build() {
    const { barmode } = this.value.toObject();
    return new BarVisualizationConfig(barmode);
  }
}
