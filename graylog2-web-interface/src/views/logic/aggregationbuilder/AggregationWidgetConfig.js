// @flow strict
import * as Immutable from 'immutable';

import { TIMESTAMP_FIELD } from 'views/Constants';
import Pivot from './Pivot';
import Series from './Series';
import VisualizationConfig from './visualizations/VisualizationConfig';
import SortConfig from './SortConfig';
import type { SeriesJson } from './Series';
import type { SortConfigJson } from './SortConfig';
import type { VisualizationConfigJson } from './visualizations/VisualizationConfig';
import type { PivotJson } from './Pivot';
import WidgetFormattingSettings from './WidgetFormattingSettings';
import type { WidgetFormattingSettingsJSON } from './WidgetFormattingSettings';
import WidgetConfig from '../widgets/WidgetConfig';

type InternalState = {
  columnPivots: Array<Pivot>,
  formattingSettings: WidgetFormattingSettings,
  rollup: boolean,
  rowPivots: Array<Pivot>,
  series: Array<Series>,
  sort: Array<SortConfig>,
  visualization: string,
  visualizationConfig: ?VisualizationConfig,
};

type AggregationWidgetConfigJson = {
  column_pivots: Array<PivotJson>,
  formatting_settings: WidgetFormattingSettingsJSON,
  rollup: boolean,
  row_pivots: Array<PivotJson>,
  series: Array<SeriesJson>,
  sort: Array<SortConfigJson>,
  visualization: string,
  visualization_config: VisualizationConfigJson,
};

export default class AggregationWidgetConfig extends WidgetConfig {
  _value: InternalState;

  constructor(columnPivots: Array<Pivot>,
    rowPivots: Array<Pivot>,
    series: Array<Series>,
    sort: Array<SortConfig>,
    visualization: string,
    rollup: boolean,
    visualizationConfig: VisualizationConfig,
    formattingSettings: WidgetFormattingSettings) {
    super();
    this._value = { columnPivots, rowPivots, series, sort, visualization, rollup, visualizationConfig, formattingSettings };
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
    if (this._value.visualizationConfig === null) {
      return undefined;
    }
    return this._value.visualizationConfig;
  }

  get formattingSettings() {
    return this._value.formattingSettings;
  }

  get isTimeline() {
    return this.rowPivots && this.rowPivots.length === 1 && this.rowPivots[0].field === TIMESTAMP_FIELD;
  }

  get isEmpty(): boolean {
    const empty = arr => !arr.length;
    return empty(this.rowPivots) && empty(this.columnPivots) && empty(this.series);
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
    const {
      columnPivots,
      formattingSettings,
      rollup,
      rowPivots,
      series,
      sort,
      visualization,
      visualizationConfig,
    } = this._value;
    return {
      column_pivots: columnPivots,
      formatting_settings: formattingSettings,
      rollup,
      row_pivots: rowPivots,
      series,
      sort,
      visualization,
      visualization_config: visualizationConfig,
    };
  }

  equals(other: any) {
    const { is } = Immutable;
    if (other instanceof AggregationWidgetConfig) {
      return ['rowPivots', 'columnPivots', 'series', 'sort', 'rollup', 'visualizationConfig']
        .every(key => is(Immutable.fromJS(this[key]), Immutable.fromJS(other[key])));
    }
    return false;
  }

  static fromJSON(value: AggregationWidgetConfigJson) {
    const {
      // eslint-disable-next-line camelcase
      column_pivots,
      // eslint-disable-next-line camelcase
      formatting_settings,
      rollup,
      // eslint-disable-next-line camelcase
      row_pivots,
      series,
      sort,
      visualization,
      // eslint-disable-next-line camelcase
      visualization_config,
    } = value;

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
      // eslint-disable-next-line camelcase
      .formattingSettings(formatting_settings === null ? undefined : WidgetFormattingSettings.fromJSON(formatting_settings))
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

  formattingSettings(value: ?WidgetFormattingSettings) {
    return new Builder(this.value.set('formattingSettings', value));
  }

  build() {
    const {
      rowPivots,
      columnPivots,
      series,
      sort,
      visualization,
      rollup,
      visualizationConfig,
      formattingSettings,
    } = this.value.toObject();

    const availableSorts = [].concat(rowPivots, columnPivots, series);
    const filteredSorts = sort.filter(s => availableSorts
      .find(availableSort => (s.field === availableSort.function || s.field === availableSort.field)));
    const computedRollup = columnPivots.length > 0 ? rollup : true;
    return new AggregationWidgetConfig(
      columnPivots,
      rowPivots,
      series,
      filteredSorts,
      visualization,
      computedRollup,
      visualizationConfig,
      formattingSettings,
    );
  }
}
