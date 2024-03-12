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

const eventsAttributes = [
  {
    attribute: 'timestamp',
    title: 'Created At',
    displayValue: (value) => <Timestamp dateTime={value} />
  },
  {
    attribute: 'alert',
    title: 'Type',
    displayValue: (value) => value
  },
  {
    attribute: 'event_definition_id',
    title: 'Event Definition',
    displayValue: (value) => <EventDefinitionName eventDefinitionId={value} />
  },
  {
    attribute: 'message',
    title: 'Description',
  }
]

export default eventsAttributes
