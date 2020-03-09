// @flow strict
export type DirectionJson = 'Ascending' | 'Descending';

export default class Direction {
  static Ascending = new Direction('Ascending');

  static Descending = new Direction('Descending');

  _direction: DirectionJson;

  constructor(direction: DirectionJson) {
    this._direction = direction;
  }

  toJSON(): DirectionJson {
    return this._direction;
  }

  get direction() {
    return this._direction;
  }

  static invert(direction: Direction) {
    return direction.direction === this.Descending.direction ? this.Ascending : this.Descending;
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
