// @flow strict
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig, { Builder } from 'views/logic/aggregationbuilder/SortConfig';

export type MessageSortConifgJson = {
  field: string,
  order: 'ASC' | 'DESC',
};

export default class MessageSortConifg {
  _value: SortConfig

  constructor(type: string, field: string, direction: Direction) {
    this._value = new SortConfig(type, field, direction);
  }

  toJSON(): MessageSortConifgJson {
    const { field, direction } = this._value;

    return {
      field,
      order: direction === Direction.Ascending ? 'ASC' : 'DESC',
    };
  }

  static fromJSON(value: MessageSortConifgJson) {
    const { field, order } = value;

    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .type(SortConfig.PIVOT_TYPE)
      .field(field)
      .direction(Direction.fromJSON(order === 'ASC' ? 'Ascending' : 'Descending'))
      .build();
  }
}
