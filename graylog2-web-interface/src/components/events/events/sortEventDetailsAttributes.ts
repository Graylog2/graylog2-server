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
import sortBy from 'lodash/sortBy';

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

// Attributes which aren't part of the order go last.
const sortEventDetailsAttributes = <A extends { id: string }>(
  attributes: ReadonlyArray<A>,
  order: ReadonlyArray<string> = EVENT_DETAILS_ATTRIBUTES_ORDER,
) => sortBy(attributes, ({ id }) => (order.includes(id) ? order.indexOf(id) : order.length));

export default sortEventDetailsAttributes;
