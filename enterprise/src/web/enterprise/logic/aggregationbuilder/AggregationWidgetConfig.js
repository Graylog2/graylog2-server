// @flow strict
import * as Immutable from 'immutable';

import { TIMESTAMP_FIELD } from 'enterprise/Constants';
import Pivot from './Pivot';
import Series from './Series';
import VisualizationConfig from './visualizations/VisualizationConfig';
import SortConfig from './SortConfig';
import type { SeriesJson } from './Series';
import type { SortConfigJson } from './SortConfig';
import type { VisualizationConfigJson } from './visualizations/VisualizationConfig';
import type { PivotJson } from './Pivot';

type InternalState = {
  columnPivots: Array<Pivot>,
  rowPivots: Array<Pivot>,
  series: Array<Series>,
  sort: Array<SortConfig>,
  visualization: string,
  rollup: boolean,
  visualizationConfig: ?VisualizationConfig,
};

type AggregationWidgetConfigJson = {
  row_pivots: Array<PivotJson>,
  column_pivots: Array<PivotJson>,
  series: Array<SeriesJson>,
  sort: Array<SortConfigJson>,
  visualization: string,
  rollup: boolean,
  visualization_config: VisualizationConfigJson,
};

export default class AggregationWidgetConfig {
  _value: InternalState;

  constructor(columnPivots: Array<Pivot>,
    rowPivots: Array<Pivot>,
    series: Array<Series>,
    sort: Array<SortConfig>,
    visualization: string,
    rollup: boolean,
    visualizationConfig: VisualizationConfig) {
    this._value = { columnPivots, rowPivots, series, sort, visualization, rollup, visualizationConfig };
  }

  get rowPivots() {
    return this._value.rowPivots;
  }

  get columnPivots() {
    return this._value.columnPivots;
  }

  get series() {
    return this._value.series;
  }

  get sort() {
    return this._value.sort;
  }

  get rollup() {
    return this._value.rollup;
  }

  get visualization() {
    return this._value.visualization;
  }

  get visualizationConfig() {
    return this._value.visualizationConfig;
  }

  get isTimeline() {
    return this.rowPivots && this.rowPivots.length === 1 && this.rowPivots[0].field === TIMESTAMP_FIELD;
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .rowPivots([])
      .columnPivots([])
      .series([])
      .sort([])
      .rollup(true);
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  toJSON() {
    const { rowPivots, columnPivots, series, sort, visualization, visualizationConfig, rollup } = this._value;
    return {
      row_pivots: rowPivots,
      column_pivots: columnPivots,
      series,
      sort,
      visualization,
      visualization_config: visualizationConfig,
      rollup,
    };
  }

  equals(other: any) {
    const { is } = Immutable;
    if (other instanceof AggregationWidgetConfig) {
      return ['rowPivots', 'columnPivots', 'series', 'sort', 'rollup']
        // $FlowFixMe: No typings for indexed access.
        .every(key => is(this[key], other[key]));
    }
    return false;
  }

  static fromJSON(value: AggregationWidgetConfigJson) {
    // eslint-disable-next-line camelcase
    const { row_pivots, column_pivots, series, sort, visualization, rollup, visualization_config } = value;

    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .columnPivots(column_pivots.map(Pivot.fromJSON))
      .rowPivots(row_pivots.map(Pivot.fromJSON))
      .series(series.map(Series.fromJSON))
      .sort(sort.map(SortConfig.fromJSON))
      .visualization(visualization)
      .rollup(rollup)
      // eslint-disable-next-line camelcase
      .visualizationConfig(visualization_config !== null ? VisualizationConfig.fromJSON(visualization, visualization_config) : null)
      .build();
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;
  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  columnPivots(pivots: Array<Pivot>) {
    return new Builder(this.value.set('columnPivots', pivots));
  }

  rowPivots(pivots: Array<Pivot>) {
    return new Builder(this.value.set('rowPivots', pivots));
  }

  series(series: Array<Series>) {
    return new Builder(this.value.set('series', series));
  }

  sort(sorts: Array<SortConfig>) {
    return new Builder(this.value.set('sort', sorts));
  }

  visualization(type: string) {
    return new Builder(this.value.set('visualization', type));
  }

  visualizationConfig(config: ?VisualizationConfig) {
    return new Builder(this.value.set('visualizationConfig', config));
  }

  rollup(rollup: boolean) {
    return new Builder(this.value.set('rollup', rollup));
  }

  build() {
    const { rowPivots, columnPivots, series, sort, visualization, rollup, visualizationConfig } = this.value.toObject();

    const availableSorts = [].concat(rowPivots, columnPivots, series);
    const filteredSorts = sort.filter(s => availableSorts.find(availableSort => (s.field === availableSort.function || s.field === availableSort.field)));
    const computedRollup = columnPivots.length > 0 ? rollup : true;
    return new AggregationWidgetConfig(columnPivots, rowPivots, series, filteredSorts, visualization, computedRollup, visualizationConfig);
  }
}
