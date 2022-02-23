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
import { groupBy } from 'lodash';

import type { ChartDefinition } from 'views/components/visualizations/ChartData';

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

type TimeFormatter = (timestamp: string) => string;

export default {
  convert(events: Array<Event>) {
    return events;
  },

  toVisualizationData(events: Events, formatTime: TimeFormatter): { eventChartData: ChartDefinition, shapes: Shapes } {
    const groupedEvents: GroupedEvents = groupBy(events, (e) => e.timestamp);

    return {
      eventChartData: this.toChartData(groupedEvents, formatTime),
      shapes: this.toShapeData(Object.keys(groupedEvents), formatTime),
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

  toChartData(events: GroupedEvents, formatTimestamp: TimeFormatter): ChartDefinition {
    const values = this.transformGroupedEvents(events);
    const xValues: Array<string> = values.map((v) => formatTimestamp(v[0]));
    const textValues: Array<string> = values.map((e) => {
      if (typeof e[1] !== 'number' && 'message' in e[1]) {
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

  toShapeData(timestamps: Array<string>, formatTimestamp: TimeFormatter): Shapes {
    return timestamps.map((timestamp) => {
      const formattedTimestamp = formatTimestamp(timestamp);

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
