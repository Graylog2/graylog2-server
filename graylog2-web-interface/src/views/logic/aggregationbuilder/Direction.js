// @flow strict
export type DirectionJson = 'Ascending' | 'Descending';

export default class Direction {
  static Ascending = new Direction('Ascending');

  static Descending = new Direction('Descending');

  direction: DirectionJson;

  constructor(direction: DirectionJson) {
    this.direction = direction;
  }

  toJSON(): DirectionJson {
    return this.direction;
  }

  get direction() {
    return this.direction;
  }

  static fromJSON(value: DirectionJson): Direction {
    return Direction.fromString(value);
  }

  static fromString(value: string): Direction {
    switch (value) {
      case 'Ascending': return Direction.Ascending;
      case 'Descending': return Direction.Descending;
      default: throw new Error(`Invalid direction: ${value}`);
    }
  }
}
