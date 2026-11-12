/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import sortEventDetailsAttributes from './sortEventDetailsAttributes';

const attributes = (ids: Array<string>) => ids.map((id) => ({ id, title: id }));
const ids = (list: Array<{ id: string }>) => list.map(({ id }) => id);

describe('sortEventDetailsAttributes', () => {
  it('sorts the attributes by the event details order', () => {
    expect(
      ids(
        sortEventDetailsAttributes(
          attributes(['fields', 'id', 'event_definition_type', 'message', 'event_definition_id']),
        ),
      ),
    ).toEqual(['message', 'event_definition_id', 'event_definition_type', 'id', 'fields']);
  });

  it('puts unknown attributes last, keeping their order', () => {
    expect(
      ids(sortEventDetailsAttributes(attributes(['second_unknown', 'fields', 'first_unknown', 'message']))),
    ).toEqual(['message', 'fields', 'second_unknown', 'first_unknown']);
  });

  it('sorts by a custom order', () => {
    expect(
      ids(sortEventDetailsAttributes(attributes(['priority', 'status', 'message']), ['message', 'status', 'priority'])),
    ).toEqual(['message', 'status', 'priority']);
  });
});
