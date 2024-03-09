import Direction from 'views/logic/aggregationbuilder/Direction';

type EventsListSortConfigJson = {
  field: string,
  direction: 'ASC' | 'DESC',
};

type InternalState = {
  field: string,
  direction: Direction,
};

export default class EventsListSortConfig {
  _value: InternalState;

  constructor(field: string, direction: Direction) {
    this._value = { field, direction };
  }

  toJSON(): EventsListSortConfigJson {
    const { field, direction } = this._value;

    return {
      field,
      direction: direction === Direction.Ascending ? 'ASC' : 'DESC',
    };
  }

  static fromJSON({ field, direction }: EventsListSortConfigJson) {
    const directionJSON = Direction.fromJSON(direction === 'ASC' ? 'Ascending' : 'Descending');

    return new EventsListSortConfig(field, directionJSON);
  }
}
