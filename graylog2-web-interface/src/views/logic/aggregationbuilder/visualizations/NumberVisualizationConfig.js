// @flow strict
import * as Immutable from 'immutable';
import VisualizationConfig from './VisualizationConfig';

type InternalState = {
  trend: boolean,
  lowerIsBetter: boolean,
};

export type NumberVisualizationConfigJSON = {
  trend: boolean,
  lower_is_better: boolean,
};

export default class NumberVisualizationConfig extends VisualizationConfig {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(
    trend: $PropertyType<InternalState, 'trend'>,
    lowerIsBetter: $PropertyType<InternalState, 'lowerIsBetter'>,
  ) {
    super();
    this._value = { trend, lowerIsBetter };
  }

  get trend() {
    return this._value.trend;
  }

  get lowerIsBetter() {
    return this._value.lowerIsBetter;
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  // eslint-disable-next-line no-undef
  static create(
    trend: $PropertyType<InternalState, 'trend'>,
    lowerIsBetter: $PropertyType<InternalState, 'lowerIsBetter'>,
  ) {
    return new NumberVisualizationConfig(trend, lowerIsBetter);
  }

  toJSON(): NumberVisualizationConfigJSON {
    const { trend, lowerIsBetter } = this._value;

    return {
      trend,
      lower_is_better: lowerIsBetter,
    };
  }

  static fromJSON(type: string, value: NumberVisualizationConfigJSON) {
    const { trend, lower_is_better: lowerIsBetter } = value;
    return NumberVisualizationConfig.create(trend, lowerIsBetter);
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  trend(value: $PropertyType<InternalState, 'trend'>) {
    return new Builder(this.value.set('trend', value));
  }

  lowerIsBetter(value: $PropertyType<InternalState, 'lowerIsBetter'>): Builder {
    return new Builder(this.value.set('lowerIsBetter', value));
  }

  build() {
    const { trend, lowerIsBetter } = this.value.toObject();
    return new NumberVisualizationConfig(trend, lowerIsBetter);
  }
}
