// @flow strict
import * as Immutable from 'immutable';

import type { SortConfigJson } from './SortConfig';
import SortConfig from './SortConfig';
import Series from './Series';
import Direction from './Direction';

export default class SeriesSortConfig extends SortConfig {
  static type = 'series';

  constructor(field: string, direction: Direction) {
    super(SeriesSortConfig.type, field, direction);
  }

  static fromJSON(value: SortConfigJson) {
    const { field, direction } = value;
    return new SeriesSortConfig(field, Direction.fromJSON(direction));
  }

  static fromSeries(series: Series) {
    return new SeriesSortConfig(series.function, Direction.Descending);
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { type, field, direction } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ type, field, direction }));
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;

  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  type(value) {
    return new Builder(this.value.set('type', value));
  }

  field(value) {
    return new Builder(this.value.set('field', value));
  }

  direction(value) {
    return new Builder(this.value.set('direction', value));
  }

  build() {
    const { field, direction } = this.value.toObject();
    return new SeriesSortConfig(field, direction);
  }
}
