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
// Order of the attributes in the event details: the event itself, where it comes from, related information,
// and the additional fields last, since they can be long. Plugins insert their attributes into this order.
export const EVENT_DETAILS_ATTRIBUTES_ORDER = [
  'message',
  'alert',
  'priority',
  'timestamp',
  'event_definition_id',
  'event_definition_type',
  'key',
  'group_by_fields',
  'remediation_steps',
  'timerange_start',
  'id',
  'tags',
  'fields',
];

const LAST_ATTRIBUTE_ID = 'fields';

/**
 * Sorts the attributes of the event details by the given order.
 * Attributes which aren't part of the order keep their relative order and go before the additional fields.
 */
const sortEventDetailsAttributes = <A extends { id: string }>(
  attributes: ReadonlyArray<A>,
  order: ReadonlyArray<string> = EVENT_DETAILS_ATTRIBUTES_ORDER,
) => {
  const position = (id: string) => {
    const index = order.indexOf(id);

    if (index !== -1) {
      return index;
    }

    const lastIndex = order.indexOf(LAST_ATTRIBUTE_ID);

    return lastIndex === -1 ? order.length : lastIndex - 0.5;
  };

  return [...attributes].sort((a, b) => position(a.id) - position(b.id));
};

export default sortEventDetailsAttributes;
