// @flow strict
import * as Immutable from 'immutable';

import type { SortConfigJson } from './SortConfig';
import SortConfig from './SortConfig';
import Pivot from './Pivot';
import Direction from './Direction';

export default class PivotSortConfig extends SortConfig {
  static type = 'pivot';

  constructor(field: string, direction: Direction) {
    super(PivotSortConfig.type, field, direction);
  }

  static fromJSON(value: SortConfigJson) {
    const { field, direction } = value;
    return new PivotSortConfig(field, Direction.fromJSON(direction));
  }

  static fromPivot(pivot: Pivot) {
    return new PivotSortConfig(pivot.field, Direction.Ascending);
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

  constructor(value: Immutable.Map = Immutable.Map()) {
    this.value = value;
  }

  type(value: string) {
    return new Builder(this.value.set('type', value));
  }

  field(value: string) {
    return new Builder(this.value.set('field', value));
  }

  direction(value: Direction) {
    return new Builder(this.value.set('direction', value));
  }

  build() {
    const { field, direction } = this.value.toObject();
    return new PivotSortConfig(field, direction);
  }
}
