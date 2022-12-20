import * as Immutable from 'immutable';

import type { AxisType, XYVisualization } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';

type InternalState = {
  axisType: AxisType,
};

export type ScatterVisualizationConfigJson = {
  axis_type?: AxisType,
};

export default class ScatterVisualizationConfig extends VisualizationConfig implements XYVisualization {
  private readonly _value: InternalState;

  constructor(axisType: InternalState['axisType'] = DEFAULT_AXIS_TYPE) {
    super();
    this._value = { axisType };
  }

  get axisType() {
    return this._value.axisType;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(axisType: InternalState['axisType']) {
    return new ScatterVisualizationConfig(axisType);
  }

  static empty() {
    return new ScatterVisualizationConfig(DEFAULT_AXIS_TYPE);
  }

  toJSON() {
    const { axisType } = this._value;

    return {
      axis_type: axisType,
    };
  }

  static fromJSON(_type: string, value: ScatterVisualizationConfigJson) {
    return ScatterVisualizationConfig.create(
      value?.axis_type ?? DEFAULT_AXIS_TYPE,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  axisType(value: InternalState['axisType']) {
    return new Builder((this.value.set('axisType', value)));
  }

  build() {
    const { interpolation, axisType } = this.value.toObject();

    return new LineVisualizationConfig(interpolation, axisType);
  }
}
