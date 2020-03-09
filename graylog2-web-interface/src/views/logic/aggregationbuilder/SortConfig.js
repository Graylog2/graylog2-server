// @flow strict
import * as Immutable from 'immutable';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import type { DirectionJson } from './Direction';
import Direction from './Direction';

export type SortConfigJson = {
  type: string,
  field: string,
  direction: DirectionJson,
};

type InternalState = {
  type: string,
  field: string,
  direction: Direction,
};

export default class SortConfig {
  static PIVOT_TYPE = 'pivot';

  static SERIES_TYPE = 'series';

  _value: InternalState;

  constructor(type: string, field: string, direction: Direction) {
    this._value = { type, field, direction };
  }

  get type() {
    return this._value.type;
  }

  get field() {
    return this._value.field;
  }

  get direction() {
    return this._value.direction;
  }

  toJSON(): SortConfigJson {
    const { type, field, direction } = this._value;

    return {
      type,
      field,
      // $FlowFixMe$: type is changed implicitly during serialisation
      direction,
    };
  }

  static fromJSON(value: SortConfigJson) {
    const { type, field, direction } = value;

    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .type(type)
      .field(field)
      .direction(Direction.fromJSON(direction))
      .build();
  }

  static __registrations: { [string]: typeof SortConfig } = {};

  static registerSubtype(type: string, implementingClass: typeof SortConfig) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }

  static fromPivot(pivot: Pivot) {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .type(this.PIVOT_TYPE)
      .field(pivot.field)
      .direction(Direction.Ascending)
      .build();
  }

  static fromSeries(series: Series) {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .type(this.SERIES_TYPE)
      .field(series.function)
      .direction(Direction.Descending)
      .build();
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { type, field, direction } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ type, field, direction }));
  }
}

type BuilderState = Immutable.Map<string, any>;
export class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
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
    const { type, field, direction } = this.value.toObject();
    return new SortConfig(type, field, direction);
  }
}
