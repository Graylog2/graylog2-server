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
import { useCallback, useContext } from 'react';

import type { Event } from 'views/logic/searchtypes/events/EventHandler';
import EventHandler from 'views/logic/searchtypes/events/EventHandler';
import UserDateTimeContext from 'contexts/UserDateTimeContext';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

const useEvents = (config: AggregationWidgetConfig, events: Event[] | undefined) => {
  const { formatTime } = useContext(UserDateTimeContext);
  const formatTimestamp = useCallback((timestamp: string) => formatTime(timestamp, 'internal'), [formatTime]);

  return (config.eventAnnotation && events)
    ? EventHandler.toVisualizationData(events, formatTimestamp)
    : { eventChartData: undefined, shapes: undefined };
};

export default useEvents;
