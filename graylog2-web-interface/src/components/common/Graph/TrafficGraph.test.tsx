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
import { render } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import GenericPlot from 'views/components/visualizations/GenericPlot';

import TrafficGraph from './TrafficGraph';

jest.mock('views/components/visualizations/GenericPlot', () => jest.fn(mockComponent('GenericPlot')));

const sampleTraffic = {
  '2026-04-07T00:00:00.000Z': 1024,
  '2026-04-08T00:00:00.000Z': 2048,
  '2026-04-09T00:00:00.000Z': 4096,
};

describe('TrafficGraph', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('configures xaxis with tickformat and hoverformat to prevent millisecond display', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { layout } = asMock(GenericPlot).mock.calls[0][0];

    expect(layout.xaxis).toMatchObject({
      type: 'date',
      tickformat: '%b %d',
      hoverformat: '%b %d, %Y',
    });
  });

  it('sets xaxis title to Date (UTC)', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { layout } = asMock(GenericPlot).mock.calls[0][0];

    expect(layout.xaxis.title).toEqual({ text: 'Date (UTC)' });
  });

  it('themes outside bar labels with the theme text color', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { chartData } = asMock(GenericPlot).mock.calls[0][0];

    expect(chartData[0].outsidetextfont).toEqual({ color: '#252D47' });
  });

  it('pins the yaxis so dragging selects a time range only, like search charts', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { layout } = asMock(GenericPlot).mock.calls[0][0];

    expect(layout.yaxis.fixedrange).toBe(true);
  });

  it('enables double-click to reset a zoomed time range', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { config } = asMock(GenericPlot).mock.calls[0][0];

    expect(config).toEqual({ doubleClick: 'reset' });
  });

  it('does not configure a plotly updatemenus widget', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    expect(asMock(GenericPlot).mock.calls[0][0].layout.updatemenus).toBeUndefined();
  });

  it('fits the yaxis to the data when zoomedToData is set', () => {
    const { rerender } = render(<TrafficGraph traffic={sampleTraffic} width={600} trafficLimit={1024 * 1024} />);

    const initialRange = asMock(GenericPlot).mock.calls[0][0].layout.yaxis.range;

    rerender(<TrafficGraph traffic={sampleTraffic} width={600} trafficLimit={1024 * 1024} zoomedToData />);

    const { calls } = asMock(GenericPlot).mock;
    const zoomedRange = calls[calls.length - 1][0].layout.yaxis.range;

    expect(zoomedRange[1]).toBeLessThan(initialRange[1]);
  });

  it('passes uiRevision through as the plotly uirevision', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} uiRevision={7} />);

    expect(asMock(GenericPlot).mock.calls[0][0].layout.uirevision).toBe(7);
  });

  it('reserves top margin so the traffic limit annotation is not clipped', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} trafficLimit={1024 * 1024} />);

    expect(asMock(GenericPlot).mock.calls[0][0].layout.margin.t).toBe(28);
  });

  it('wires onUserZoom to the plot zoom event', () => {
    const onUserZoom = jest.fn();

    render(<TrafficGraph traffic={sampleTraffic} width={600} onUserZoom={onUserZoom} />);

    expect(asMock(GenericPlot).mock.calls[0][0].onZoom).toBe(onUserZoom);
  });

  it('wires onUserZoomReset to the plot reset event', () => {
    const onUserZoomReset = jest.fn();

    render(<TrafficGraph traffic={sampleTraffic} width={600} onUserZoomReset={onUserZoomReset} />);

    expect(asMock(GenericPlot).mock.calls[0][0].onZoomReset).toBe(onUserZoomReset);
  });
});
