import Immutable from 'immutable';
import Pivot from './Pivot';
import Series from './Series';

export default class AggregationWidgetConfig {
  constructor(columnPivots, rowPivots, series, sort, visualization, rollup) {
    this._value = { columnPivots, rowPivots, series, sort, visualization, rollup };
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

  toObject() {
    const { rowPivots, columnPivots, series, sort, visualization, rollup } = this._value;
    return {
      rowPivots: rowPivots.slice(0),
      columnPivots: columnPivots.slice(0),
      series: series.slice(0),
      sort: sort.slice(0),
      visualization,
      rollup,
    };
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
    const { rowPivots, columnPivots, series, sort, visualization, rollup } = this._value;
    return {
      row_pivots: rowPivots,
      column_pivots: columnPivots,
      series,
      sort,
      visualization,
      rollup,
    };
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { row_pivots, column_pivots, series, sort, visualization, rollup } = value;

    return new AggregationWidgetConfig(column_pivots.map(Pivot.fromJSON), row_pivots.map(Pivot.fromJSON), series.map(Series.fromJSON), sort, visualization, rollup);
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  columnPivots(pivots) {
    return new Builder(this.value.set('columnPivots', pivots));
  }

  rowPivots(pivots) {
    return new Builder(this.value.set('rowPivots', pivots));
  }

  series(series) {
    return new Builder(this.value.set('series', series));
  }

  sort(sorts) {
    return new Builder(this.value.set('sort', sorts));
  }

  visualization(type) {
    return new Builder(this.value.set('visualization', type));
  }

  rollup(rollup) {
    return new Builder(this.value.set('rollup', rollup));
  }

  build() {
    const { rowPivots, columnPivots, series, sort, visualization, rollup } = this.value.toObject();
    return new AggregationWidgetConfig(columnPivots, rowPivots, series, sort, visualization, rollup);
  }
}
