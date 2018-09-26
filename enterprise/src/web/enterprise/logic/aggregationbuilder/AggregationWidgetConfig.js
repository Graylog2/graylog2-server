import Immutable from 'immutable';
import Pivot from './Pivot';
import Series from './Series';

export default class AggregationWidgetConfig {
  constructor(columnPivots, rowPivots, series, sort, visualization) {
    this._value = { columnPivots, rowPivots, series, sort, visualization };
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

  get visualization() {
    return this._value.visualization;
  }

  toObject() {
    const { rowPivots, columnPivots, series, sort, visualization } = this._value;
    return {
      rowPivots: rowPivots.slice(0),
      columnPivots: columnPivots.slice(0),
      series: series.slice(0),
      sort: sort.slice(0),
      visualization,
    };
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .rowPivots([])
      .columnPivots([])
      .series([])
      .sort([]);
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  toJSON() {
    const { rowPivots, columnPivots, series, sort, visualization } = this._value;
    return {
      row_pivots: rowPivots,
      column_pivots: columnPivots,
      series,
      sort,
      visualization,
    };
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { row_pivots, column_pivots, series, sort, visualization } = value;

    return new AggregationWidgetConfig(column_pivots.map(Pivot.fromJSON), row_pivots.map(Pivot.fromJSON), series.map(Series.fromJSON), sort, visualization);
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

  build() {
    const { rowPivots, columnPivots, series, sort, visualization } = this.value.toObject();
    return new AggregationWidgetConfig(columnPivots, rowPivots, series, sort, visualization);
  }
}
