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

import type { WidgetComponentProps } from 'views/types';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import { LIST_MODE, NUMBER_MODE } from 'views/logic/widgets/events/EventsWidgetConfig';
import type { EventsListResult } from 'views/components/widgets/events/types';
import EventsList from 'views/components/widgets/events/EventsList';

import EventsNumber from './EventsNumber';

const EventsVisualization = (props: WidgetComponentProps<EventsWidgetConfig, EventsListResult>) => {
  const { config } = props;

  if (config.mode === LIST_MODE) {
    return <EventsList {...props} />;
  }

  if (config.mode === NUMBER_MODE) {
    return <EventsNumber {...props} />;
  }

  return <div>Unknown widget visualization type {config.mode ?? 'undefined'}</div>;
};

export default EventsVisualization;
