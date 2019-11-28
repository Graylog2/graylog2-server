// @flow strict
import { groupBy } from 'lodash';
import WidgetFormattingSettings from 'views/logic/aggregationbuilder/WidgetFormattingSettings';
import type { Event } from './EventHandler';
import EventHandler from './EventHandler';

const groupByTimestamp = (es => groupBy(es, e => e.timestamp));

describe('EventHandler convert', () => {
  const event: Event = {
    alert: false,
    id: '01DSMMX5M4ER2MR02DDB2JS93W',
    message: 'This is a emergency. Please stay calm.',
    streams: ['5cdab2293d27467fbe9e8a72'],
    timestamp: '2019-11-14T08:53:35.000Z',
  };

  it('should convert events to chart data and shapes', () => {
    const result = EventHandler.toVisualizationData([event]);
    expect(result).toMatchSnapshot();
  });

  it('should convert events to char data', () => {
    const result = EventHandler.toChartData(groupByTimestamp([event]));
    expect(result).toEqual({
      hovertemplate: '%{text}',
      marker: {
        color: '#d3d3d3',
        size: 5,
      },
      mode: 'markers',
      opacity: 0.5,
      name: 'Alerts',
      type: 'scatter',
      x: ['2019-11-14T08:53:35.000Z'],
      y: [0],
      text: ['This is a emergency. Please stay calm.'],
    });
  });

  it('should group duplicate events by timestamp and convert events to char data', () => {
    const result = EventHandler.toChartData(groupByTimestamp([event, event, event]));
    expect(result).toEqual({
      hovertemplate: '%{text}',
      marker: {
        color: '#d3d3d3',
        size: 5,
      },
      mode: 'markers',
      opacity: 0.5,
      name: 'Alerts',
      type: 'scatter',
      x: ['2019-11-14T08:53:35.000Z'],
      y: [0],
      text: ['3 alerts occurred.'],
    });
  });

  it('should convert events to shape data', () => {
    const result = EventHandler.toShapeData([event.timestamp]);
    expect(result[0]).toEqual({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: '2019-11-14T08:53:35.000Z',
      x1: '2019-11-14T08:53:35.000Z',
      opacity: 0.5,
      line: {
        color: '#d3d3d3',
      },
    });
  });

  it('should convert events to shape data with custom color', () => {
    const widgetFormattingSettings = WidgetFormattingSettings.create({ Alerts: '#ffffff' });
    const result = EventHandler.toShapeData([event.timestamp], widgetFormattingSettings);
    expect(result[0]).toEqual({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: '2019-11-14T08:53:35.000Z',
      x1: '2019-11-14T08:53:35.000Z',
      opacity: 0.5,
      line: {
        color: '#ffffff',
      },
    });
  });
});
