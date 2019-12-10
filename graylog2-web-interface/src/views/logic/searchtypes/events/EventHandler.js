// @flow strict
import { groupBy } from 'lodash';
import WidgetFormattingSettings from 'views/logic/aggregationbuilder/WidgetFormattingSettings';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';

export type Event = {
  id: string,
  timestamp: string,
  message: string,
  alert: boolean,
  streams: Array<string>,
};

export type Events = Array<Event>;

type GroupedEvents = { [string]: Events };

type Shape = {
  type: 'line',
  y0: number,
  y1: number,
  x0: string,
  x1: string,
  opacity: number,
  line: {
    color: string,
  }
};

export type Shapes = Array<Shape>;
const eventsDisplayName = 'Alerts';
const defaultColor = '#d3d3d3';

export default {
  convert(events: Array<Event>) {
    return events;
  },

  toVisualizationData(events: Events = [],
    formattingSettings: WidgetFormattingSettings = WidgetFormattingSettings.create({})): { eventChartData: ChartDefinition, shapes: Shapes } {
    const groupedEvents: GroupedEvents = groupBy(events, e => e.timestamp);
    return {
      eventChartData: this.toChartData(groupedEvents, formattingSettings),
      shapes: this.toShapeData(Object.keys(groupedEvents), formattingSettings),
    };
  },

  transformGroupedEvents(events: GroupedEvents): [[string, Event | number]] {
    // $FlowFixMe Object.entries has only mixed values.
    return Object.entries(events).map(([timestamp:string, eventArray:Events]) => {
      if (!(eventArray instanceof Array)) {
        throw new Error('Unexpected data type');
      }

      if (eventArray.length > 1) {
        return [timestamp, eventArray.length];
      }

      return [timestamp, eventArray[0]];
    });
  },

  toChartData(events: GroupedEvents, formattingSettings: WidgetFormattingSettings): ChartDefinition {
    const { chartColors } = formattingSettings;
    const chartColor = chartColors[eventsDisplayName] || defaultColor;
    const values = this.transformGroupedEvents(events);
    const xValues: Array<string> = values.map(v => v[0]);
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
      marker: {
        size: 5,
        color: chartColor,
      },
    };
  },

  toShapeData(timestamps: Array<string>, formattingSettings: WidgetFormattingSettings = WidgetFormattingSettings.create({})): Shapes {
    const { chartColors } = formattingSettings;
    const shapeColor = chartColors[eventsDisplayName] || defaultColor;
    return timestamps.map(timestamp => ({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: timestamp,
      x1: timestamp,
      opacity: 0.5,
      line: {
        color: shapeColor,
      },
    }));
  },
};
