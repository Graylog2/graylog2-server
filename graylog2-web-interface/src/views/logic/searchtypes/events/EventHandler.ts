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
import moment from 'moment-timezone';
import { groupBy } from 'lodash';

import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import CombinedProvider from 'injection/CombinedProvider';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

export type Event = {
  id: string,
  timestamp: string,
  message: string,
  alert: boolean,
  streams: Array<string>,
};

export type Events = Array<Event>;

export const EVENT_COLOR = '#d3d3d3';

type GroupedEvents = { [key: string]: Events };

type Shape = {
  type: 'line',
  y0: number,
  y1: number,
  x0: string,
  x1: string,
  opacity: number,
  line?: {
    color: string,
  },
};

export type Shapes = Array<Shape>;

export const eventsDisplayName = 'Alerts';

const formatTimestamp = (timestamp, tz = 'UTC'): string => {
  // the `true` parameter prevents returning the iso string in UTC (http://momentjs.com/docs/#/displaying/as-iso-string/)
  return moment(timestamp).tz(tz ?? 'UTC').toISOString(true);
};

export default {
  convert(events: Array<Event>) {
    return events;
  },

  toVisualizationData(events: Events = []): { eventChartData: ChartDefinition, shapes: Shapes } {
    const currentUser = CurrentUserStore.get();
    const tz = currentUser ? currentUser.timezone : 'UTC';

    const groupedEvents: GroupedEvents = groupBy(events, (e) => e.timestamp);

    return {
      eventChartData: this.toChartData(groupedEvents, tz),
      shapes: this.toShapeData(Object.keys(groupedEvents), tz),
    };
  },

  transformGroupedEvents(events: GroupedEvents): [string, Event | number][] {
    return Object.entries(events).map(([timestamp, eventArray]) => {
      if (!(eventArray instanceof Array)) {
        throw new Error('Unexpected data type');
      }

      if (eventArray.length > 1) {
        return [timestamp, eventArray.length];
      }

      return [timestamp, eventArray[0]];
    });
  },

  toChartData(events: GroupedEvents, tz: string): ChartDefinition {
    const values = this.transformGroupedEvents(events);
    const xValues: Array<string> = values.map((v) => formatTimestamp(v[0], tz));
    const textValues: Array<string> = values.map((e) => {
      if (Object.prototype.hasOwnProperty.call(e[1], 'message')) {
        return e[1].message;
      }

      return `${e[1]} alerts occurred.`;
    });
    const yValues: Array<number> = values.map(() => 0);

    return {
      hovertemplate: '%{text}',
      mode: 'markers',
      name: eventsDisplayName,
      type: 'scatter',
      opacity: 0.5,
      x: xValues,
      y: yValues,
      text: textValues,
    };
  },

  toShapeData(timestamps: Array<string>, tz: string): Shapes {
    return timestamps.map((timestamp) => {
      const formattedTimestamp = formatTimestamp(timestamp, tz);

      return {
        layer: 'below',
        type: 'line',
        yref: 'paper',
        y0: 0,
        y1: 1,
        x0: formattedTimestamp,
        x1: formattedTimestamp,
        opacity: 0.5,
      };
    });
  },
};
