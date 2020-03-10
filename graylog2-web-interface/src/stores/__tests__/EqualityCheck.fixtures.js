// @flow strict
import { List, Map } from 'immutable';

type MixedMapsAndObjects = { [string]: Map<string, { [string]: Map<string, number> }> };

export const mapWithObject = (): Map<string, { [string]: number }> => Map({ foo: { bar: 42 } });
export const listWithObject = (): List<{ [string]: { [string]: number } }> => List([{ foo: { bar: 42 } }]);
export const objectWithMap = (): { [string]: Map<string, number> } => ({ foo: Map({ bar: 42 }) });
export const arrayOfMaps = (): Array<Map<string, number>> => [Map({ foo: 23 }), Map({ bar: 42 })];
export const mixedMapsAndObjects = (): MixedMapsAndObjects => ({ foo: Map({ bar: { baz: Map({ qux: 42 }) } }) });

export class AlwaysEqual {
  equals = () => {
    return true;
  };
}

export class NeverEqual {
  equals = () => {
    return false;
  };
}

export class NonValueClass {
  value: number;

  constructor(value: number) {
    this.value = value;
  }
}
