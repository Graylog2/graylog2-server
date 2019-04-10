// @flow strict
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
    const { type } = value;
    const implementingClass = SortConfig.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(value);
    }

    throw new Error(`Invalid sort config type specified: ${type}`);
  }

  static __registrations: { [string]: typeof SortConfig } = {};

  static registerSubtype(type: string, implementingClass: typeof SortConfig) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }
}
