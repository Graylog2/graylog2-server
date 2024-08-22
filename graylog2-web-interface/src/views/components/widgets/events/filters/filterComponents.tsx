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

import EventDefinitionName from 'views/components/widgets/events/filters/EventDefinitionName';
import EventDefinitionFilter from 'views/components/widgets/events/filters/EventDefinitionFilter';
import EventTypeFilter from 'views/components/widgets/events/filters/EventTypeFilter';
import type { FilterComponents } from 'views/components/widgets/overview-configuration/filters/types';
import EventPriorityFilter from 'views/components/widgets/events/filters/EventPriorityFilter';
import PriorityName from 'components/events/events/PriorityName';

const filterComponents: FilterComponents = [
  {
    configuration: (selectedValues, editValue: string, onChange: (newValue: string) => void) => (
      <EventDefinitionFilter value={editValue} onSelect={(newValue) => onChange(newValue)} selectedValues={selectedValues} />
    ),
    attribute: 'event_definition_id',
    renderValue: (value) => <EventDefinitionName eventDefinitionId={value} displayAsLink={false} />,
    allowMultipleValues: true,
  },
  {
    attribute: 'alert',
    configuration: (selectedValues, _editValue: string, onChange: (newValue: string) => void) => (
      <EventTypeFilter onSelect={onChange} selectedValues={selectedValues} />
    ),
    renderValue: (isAlert: 'true' | 'false') => (isAlert === 'true' ? 'Alert' : 'Event'),
  },
  {
    attribute: 'priority',
    configuration: (selectedValues, _editValue: string, onChange: (newValue: string) => void) => (
      <EventPriorityFilter onSelect={onChange} selectedValues={selectedValues} />
    ),
    renderValue: (priority: string) => <PriorityName priority={priority} />,
  },
];

export default filterComponents;
