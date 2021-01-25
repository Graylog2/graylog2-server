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

import WidgetFormattingSettings from 'views/logic/aggregationbuilder/WidgetFormattingSettings';

import type { Event } from './EventHandler';
import EventHandler from './EventHandler';

const groupByTimestamp = ((es) => groupBy(es, (e) => e.timestamp));

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
    const result = EventHandler.toChartData(groupByTimestamp([event]), WidgetFormattingSettings.builder().build(), 'UTC');

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
      x: ['2019-11-14T08:53:35.000+00:00'],
      y: [0],
      text: ['This is a emergency. Please stay calm.'],
    });
  });

  it('should convert events to char data and use new timezone', () => {
    const result = EventHandler.toChartData(groupByTimestamp([event]), WidgetFormattingSettings.builder().build(), 'CET');

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
      x: ['2019-11-14T09:53:35.000+01:00'],
      y: [0],
      text: ['This is a emergency. Please stay calm.'],
    });
  });

  it('should group duplicate events by timestamp and convert events to char data', () => {
    const result = EventHandler.toChartData(groupByTimestamp([event, event, event]), WidgetFormattingSettings.builder().build(), 'UTC');

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
      x: ['2019-11-14T08:53:35.000+00:00'],
      y: [0],
      text: ['3 alerts occurred.'],
    });
  });

  it('should keep the color set by the user', () => {
    const result = EventHandler.toChartData(groupByTimestamp([event]), WidgetFormattingSettings.builder()
      .chartColors({ Alerts: '#ffffff' })
      .build(), 'UTC');

    expect(result).toEqual({
      hovertemplate: '%{text}',
      marker: {
        color: '#ffffff',
        size: 5,
      },
      mode: 'markers',
      opacity: 0.5,
      name: 'Alerts',
      type: 'scatter',
      x: ['2019-11-14T08:53:35.000+00:00'],
      y: [0],
      text: ['This is a emergency. Please stay calm.'],
    });
  });

  it('should convert events to shape data', () => {
    const result = EventHandler.toShapeData([event.timestamp], undefined, 'UTC');

    expect(result[0]).toEqual({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: '2019-11-14T08:53:35.000+00:00',
      x1: '2019-11-14T08:53:35.000+00:00',
      opacity: 0.5,
      line: {
        color: '#d3d3d3',
      },
    });
  });

  it('should convert events to shape data with new timezone', () => {
    const result = EventHandler.toShapeData([event.timestamp], undefined, 'CET');

    expect(result[0]).toEqual({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: '2019-11-14T09:53:35.000+01:00',
      x1: '2019-11-14T09:53:35.000+01:00',
      opacity: 0.5,
      line: {
        color: '#d3d3d3',
      },
    });
  });

  it('should convert events to shape data with custom color', () => {
    const widgetFormattingSettings = WidgetFormattingSettings.create({ Alerts: '#ffffff' });
    const result = EventHandler.toShapeData([event.timestamp], widgetFormattingSettings, 'UTC');

    expect(result[0]).toEqual({
      layer: 'below',
      type: 'line',
      yref: 'paper',
      y0: 0,
      y1: 1,
      x0: '2019-11-14T08:53:35.000+00:00',
      x1: '2019-11-14T08:53:35.000+00:00',
      opacity: 0.5,
      line: {
        color: '#ffffff',
      },
    });
  });
});
