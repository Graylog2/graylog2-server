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

import { Timestamp } from 'components/common';
import EventDefinitionName from 'views/components/widgets/events/filters/EventDefinitionName';
import EventTypeLabel from 'components/events/events/EventTypeLabel';
import type { Attributes } from 'views/components/widgets/overview-configuration/filters/types';
import PriorityName from 'components/events/events/PriorityName';

const eventsAttributes: Attributes = [
  {
    attribute: 'timestamp',
    displayValue: (value: string) => <Timestamp dateTime={value} />,
    sortable: true,
    title: 'Created At',
  },
  {
    attribute: 'alert',
    displayValue: (value: boolean) => <EventTypeLabel isAlert={value} />,
    sortable: true,
    title: 'Type',
  },
  {
    attribute: 'event_definition_id',
    displayValue: (value: string) => <EventDefinitionName eventDefinitionId={value} />,
    sortable: true,
    title: 'Event Definition',
  },
  {
    attribute: 'priority',
    displayValue: (value: number) => <PriorityName priority={value} />,
    sortable: true,
    title: 'Priority',
  },
  {
    attribute: 'key',
    title: 'Key',
  },
  {
    attribute: 'message',
    title: 'Description',
  },
];

export default eventsAttributes;
