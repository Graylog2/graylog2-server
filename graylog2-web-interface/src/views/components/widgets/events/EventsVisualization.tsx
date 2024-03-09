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
