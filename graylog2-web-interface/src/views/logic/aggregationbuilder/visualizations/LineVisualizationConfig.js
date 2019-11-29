// @flow strict
import * as Immutable from 'immutable';
import VisualizationConfig from './VisualizationConfig';
import type { InterpolationMode } from './Interpolation';

type InternalState = {
  interpolation: InterpolationMode,
};

export type LineVisualizationConfigJSON = {
  interpolation: InterpolationMode,
};

export default class LineVisualizationConfig extends VisualizationConfig {
  _value: InternalState;

  constructor(interpolation: $PropertyType<InternalState, 'interpolation'>) {
    super();
    this._value = { interpolation };
  }

  get interpolation() {
    return this._value.interpolation;
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(interpolation: $PropertyType<InternalState, 'interpolation'>) {
    return new LineVisualizationConfig(interpolation);
  }

  static empty() {
    return new LineVisualizationConfig('linear');
  }

  toJSON() {
    const { interpolation } = this._value;

    return { interpolation };
  }

  static fromJSON(type: string, value: LineVisualizationConfigJSON = { interpolation: 'linear' }) {
    const { interpolation = 'linear' } = value;
    return LineVisualizationConfig.create(interpolation);
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  interpolation(value: $PropertyType<InternalState, 'interpolation'>) {
    return new Builder(this.value.set('interpolation', value));
  }

  build() {
    const { interpolation } = this.value.toObject();
    return new LineVisualizationConfig(interpolation);
  }
}
