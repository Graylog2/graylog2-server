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

import VisualizationConfig from './VisualizationConfig';

export type TrendPreference = 'LOWER' | 'NEUTRAL' | 'HIGHER';
export type NumberAlignment = 'center' | 'bottom-right' | 'bottom-left';

type InternalState = {
  trend: boolean;
  trendPreference: TrendPreference;
  alignment: NumberAlignment | undefined;
};

export type NumberVisualizationConfigJSON = {
  trend: boolean;
  trend_preference: TrendPreference;
  alignment?: NumberAlignment;
};

export default class NumberVisualizationConfig extends VisualizationConfig {
  private readonly _value: InternalState;

  constructor(
    trend: InternalState['trend'],
    trendPreference: InternalState['trendPreference'],
    alignment: InternalState['alignment'] = 'bottom-right',
  ) {
    super();
    this._value = { trend, trendPreference, alignment };
  }

  get trend() {
    return this._value.trend;
  }

  get trendPreference() {
    return this._value.trendPreference;
  }

  // Where the value/trend are anchored within the widget. Defaults to 'bottom-right' for existing widgets.
  get alignment() {
    return this._value.alignment;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(
    trend: InternalState['trend'] = false,
    trendPreference: InternalState['trendPreference'] = 'NEUTRAL',
    alignment: InternalState['alignment'] = 'bottom-right',
  ) {
    return new NumberVisualizationConfig(trend, trendPreference, alignment);
  }

  static empty() {
    return NumberVisualizationConfig.create(false, 'NEUTRAL');
  }

  toJSON(): NumberVisualizationConfigJSON {
    const { trend, trendPreference, alignment } = this._value;

    return {
      trend,
      trend_preference: trendPreference,
      alignment,
    };
  }

  equalsForSearch(other: any) {
    return other && 'trend' in other && other.trend === this.trend;
  }

  static fromJSON(_type: string, value: NumberVisualizationConfigJSON) {
    const { trend, trend_preference: trendPreference, alignment } = value;

    return NumberVisualizationConfig.create(trend, trendPreference, alignment);
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  trend(value: InternalState['trend']) {
    return new Builder(this.value.set('trend', value));
  }

  trendPreference(value: InternalState['trendPreference']): Builder {
    return new Builder(this.value.set('trendPreference', value));
  }

  alignment(value: InternalState['alignment']): Builder {
    return new Builder(this.value.set('alignment', value));
  }

  build() {
    const { trend, trendPreference, alignment } = this.value.toObject();

    return new NumberVisualizationConfig(trend, trendPreference, alignment);
  }
}
