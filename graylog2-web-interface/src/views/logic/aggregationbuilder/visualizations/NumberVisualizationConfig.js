// @flow strict
import * as Immutable from 'immutable';

import VisualizationConfig from './VisualizationConfig';

export type TrendPreference = 'LOWER' | 'NEUTRAL' | 'HIGHER';

type InternalState = {
  trend: boolean,
  trendPreference: TrendPreference,
};

export type NumberVisualizationConfigJSON = {
  trend: boolean,
  trend_preference: TrendPreference,
};

export default class NumberVisualizationConfig extends VisualizationConfig {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(
    trend: $PropertyType<InternalState, 'trend'>,
    trendPreference: $PropertyType<InternalState, 'trendPreference'>,
  ) {
    super();
    this._value = { trend, trendPreference };
  }

  get trend() {
    return this._value.trend;
  }

  get trendPreference() {
    return this._value.trendPreference;
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map((this._value: { [string]: any })));
  }

  static create(
    trend: $PropertyType<InternalState, 'trend'> = false,
    lowerIsBetter: $PropertyType<InternalState, 'trendPreference'> = 'NEUTRAL',
  ) {
    return new NumberVisualizationConfig(trend, lowerIsBetter);
  }

  static empty() {
    return NumberVisualizationConfig.create(false, 'NEUTRAL');
  }

  toJSON(): NumberVisualizationConfigJSON {
    const { trend, trendPreference } = this._value;

    return {
      trend,
      trend_preference: trendPreference,
    };
  }

  static fromJSON(type: string, value: NumberVisualizationConfigJSON) {
    const { trend, trend_preference: trendPreference } = value;

    return NumberVisualizationConfig.create(trend, trendPreference);
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

  trendPreference(value: $PropertyType<InternalState, 'trendPreference'>): Builder {
    return new Builder(this.value.set('trendPreference', value));
  }

  build() {
    const { trend, trendPreference } = this.value.toObject();

    return new NumberVisualizationConfig(trend, trendPreference);
  }
}
