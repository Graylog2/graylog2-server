// @flow strict
import Direction from 'views/logic/aggregationbuilder/Direction';

type MessageSortConfigJson = {
  field: string,
  order: 'ASC' | 'DESC',
};

type InternalState = {
  field: string,
  direction: Direction,
};

export default class MessageSortConfig {
  _value: InternalState

  constructor(field: string, direction: Direction) {
    this._value = { field, direction };
  }

  toJSON(): MessageSortConfigJson {
    const { field, direction } = this._value;
    return {
      field,
      order: direction === Direction.Ascending ? 'ASC' : 'DESC',
    };
  }

  static fromJSON({ field, order }: MessageSortConfigJson) {
    const direction = Direction.fromJSON(order === 'ASC' ? 'Ascending' : 'Descending');
    return new MessageSortConfig(field, direction);
  }
}
