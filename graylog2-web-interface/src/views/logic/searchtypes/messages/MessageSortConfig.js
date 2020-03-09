// @flow strict
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig, { Builder } from 'views/logic/aggregationbuilder/SortConfig';

export type MessageSortConfigJson = {
  field: string,
  order: 'ASC' | 'DESC',
};

export default class MessageSortConfig {
  _value: SortConfig

  constructor(type: string, field: string, direction: Direction) {
    this._value = new SortConfig(type, field, direction);
  }

  toJSON(): MessageSortConfigJson {
    const { field, direction } = this._value;

    return {
      field,
      order: direction === Direction.Ascending ? 'ASC' : 'DESC',
    };
  }

  static fromJSON(value: MessageSortConfigJson) {
    const { field, order } = value;

    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .type(SortConfig.PIVOT_TYPE)
      .field(field)
      .direction(Direction.fromJSON(order === 'ASC' ? 'Ascending' : 'Descending'))
      .build();
  }
}
