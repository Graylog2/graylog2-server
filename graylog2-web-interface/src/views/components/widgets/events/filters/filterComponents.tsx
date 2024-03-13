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
import DateFilter from 'views/components/widgets/overview-configuration/filters/DateFilter';

const filterComponents = [
  {
    configuration: (selectedValues, editValue: string, onChange: (newValue: string) => void) => (
      <EventDefinitionFilter value={editValue} onSelect={(newValue) => onChange(newValue)} selectedValues={selectedValues} />
    ),
    attribute: 'event_definition_id',
    renderValue: (value) => <EventDefinitionName eventDefinitionId={value} />,
  },
  {
    attribute: 'alert',
    configuration: (_selectedValues, editValue: string, onChange: (newValue: string) => void) => (
      <EventTypeFilter value={editValue} onSelect={(newValue) => onChange(newValue)} />
    ),
  },
  {
    attribute: 'timestamp',
    configuration: (_selectedValues, editValue: Array<string>, onChange: (newValue: Array<string>, shouldSubmit: boolean) => void) => (
      <DateFilter values={editValue} onChange={(newValue) => onChange(newValue, false)} />
    ),
    valueFromConfig: (value: string) => (value ? value.split(',') : []),
    renderValue: (values: string) => values.replace(',', ' to '),
  },
];

export default filterComponents;
