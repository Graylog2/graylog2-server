// @flow strict
import { uniq } from 'lodash';
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

export default {
  convert(events: Array<Event>) {
    return events;
  },

  toVisualizationData(events: Events = [],
    formattingSettings: WidgetFormattingSettings = WidgetFormattingSettings.create({})): { eventChartData: ChartDefinition, shapes: Shapes } {
    return {
      eventChartData: this.toChartData(events),
      shapes: this.toShapeData(events, formattingSettings),
    };
  },

  toChartData(events: Events = []): ChartDefinition {
    const values: Array<[string, string]> = uniq(events.map(event => [event.timestamp, event.message]));
    const xValues: Array<string> = values.map(v => v[0]);
    const textValues: Array<string> = values.map(v => v[1]);
    const yValues: Array<number> = values.map(() => 0);
    return {
      mode: 'markers',
      name: eventsDisplayName,
      type: 'scatter',
      opacity: 0.5,
      x: xValues,
      y: yValues,
      text: textValues,
      marker: {
        size: 3,
        color: '#d3d3d3',
      },
    };
  },

  toShapeData(events: Events = [], formattingSettings: WidgetFormattingSettings = WidgetFormattingSettings.create({})): Shapes {
    const { chartColors } = formattingSettings;
    const shapeColor = chartColors[eventsDisplayName] || '#d3d3d3';
    return events.map(event => ({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: event.timestamp,
      x1: event.timestamp,
      opacity: 0.5,
      line: {
        color: shapeColor,
      },
    }));
  },
};
