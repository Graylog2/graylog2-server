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

import * as React from 'react';

import type { Slice, SliceRenderers } from 'components/common/PaginatedEntityTable/slicing/Slicing';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import PriorityName from 'components/events/events/PriorityName';
import EventTypeLabel from 'components/events/events/EventTypeLabel';
import StringUtils from 'util/StringUtils';

const extendPrioritySlices = (slices: Array<Slice>) => {
  const countsByValue = new Map(slices.map((slice) => [String(slice.value), slice.count]));

  return Object.entries(EventDefinitionPriorityEnum.properties).map(([value, { name }]) => ({
    value,
    title: StringUtils.capitalizeFirstLetter(name),
    count: countsByValue.get(String(value)) ?? 0,
  }));
};

const extendTypeSlices = (slices: Array<Slice>) => {
  const countsByValue = new Map(slices.map((slice) => [String(slice.value), slice.count]));
  const values = [
    { value: 'true', title: 'Alert' },
    { value: 'false', title: 'Event' },
  ];

  return values.map(({ value, title }) => ({
    value,
    title,
    count: countsByValue.get(value) ?? 0,
  }));
};

const eventsSliceRenderers: SliceRenderers = {
  priority: {
    extendSlices: extendPrioritySlices,
    render: (value) => <PriorityName priority={value} />,
  },
  alert: {
    extendSlices: extendTypeSlices,
    render: (value) => <EventTypeLabel isAlert={String(value) === 'true'} />,
  },
};

export default eventsSliceRenderers;
