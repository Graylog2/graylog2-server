// @flow strict
import * as Immutable from 'immutable';
import VisualizationConfig from './VisualizationConfig';
import type { InterpolationMode } from './Interpolation';

type InternalState = {
  interpolation: InterpolationMode,
};

export type AreaVisualizationConfigJSON = {
  interpolation: InterpolationMode,
};

export default class AreaVisualizationConfig extends VisualizationConfig {
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
    return new AreaVisualizationConfig(interpolation);
  }

  static empty() {
    return new AreaVisualizationConfig('linear');
  }

  toJSON() {
    const { interpolation } = this._value;

    return { interpolation };
  }

  static fromJSON(type: string, value: AreaVisualizationConfigJSON = { interpolation: 'linear' }) {
    const { interpolation = 'linear' } = value;
    return AreaVisualizationConfig.create(interpolation);
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
    return new AreaVisualizationConfig(interpolation);
  }
}
