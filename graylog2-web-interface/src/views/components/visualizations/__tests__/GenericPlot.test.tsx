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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ColorMapper from 'views/components/visualizations/ColorMapper';
import AsyncPlot from 'views/components/visualizations/plotly/AsyncPlot';
import asMock from 'helpers/mocking/AsMock';

import ChartColorContext from '../ChartColorContext';
import type { ChartConfig } from '../GenericPlot';
import GenericPlot from '../GenericPlot';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

// We need to mock the Popover, because it implements the GraylogThemeProvider which does not render its children
// without CurrentUser & UserPreferences
jest.mock('components/bootstrap/Popover');

jest.mock('views/components/visualizations/plotly/AsyncPlot', () => jest.fn());
jest.mock('components/common/ColorPicker', () => 'color-picker');

describe('GenericPlot', () => {
  describe('adds onRelayout handler', () => {
    it('calling onZoom prop if axis have changed', async () => {
      asMock(AsyncPlot).mockImplementation(({ onRelayout }) => (
        <button type="button" onClick={() => onRelayout({ 'xaxis.range[0]': 23, 'xaxis.range[1]': 42 })}>
          Zoom
        </button>
      ));

      const onZoom = jest.fn();
      render(<GenericPlot chartData={[]} onZoom={onZoom} />);

      await userEvent.click(await screen.findByRole('button', { name: 'Zoom' }));

      expect(onZoom).toHaveBeenCalled();
      expect(onZoom).toHaveBeenCalledWith(23, 42);
    });

    it('not calling onZoom prop if axis have not changed', async () => {
      asMock(AsyncPlot).mockImplementation(({ onRelayout }) => (
        <button type="button" onClick={() => onRelayout({ autosize: true })}>
          Zoom
        </button>
      ));

      const onZoom = jest.fn();
      render(<GenericPlot chartData={[]} onZoom={onZoom} />);

      await userEvent.click(await screen.findByRole('button', { name: 'Zoom' }));

      expect(onZoom).not.toHaveBeenCalled();
    });
  });

  describe('layout handling', () => {
    it('configures legend to be displayed horizontally', () => {
      render(<GenericPlot chartData={[]} />);

      expect(AsyncPlot).toHaveBeenCalledWith(
        expect.objectContaining({
          layout: expect.objectContaining({
            legend: expect.objectContaining({
              orientation: 'h',
            }),
          }),
        }),
        {},
      );
    });

    it('merges in passed layout property', () => {
      const layout = { height: 1000 };
      render(<GenericPlot chartData={[]} layout={layout} />);

      expect(AsyncPlot).toHaveBeenCalledWith(
        expect.objectContaining({
          layout: expect.objectContaining({
            height: 1000,
            autosize: true,
          }),
        }),
        {},
      );
    });

    it('configures resize handler to be used', () => {
      render(<GenericPlot chartData={[]} />);

      expect(AsyncPlot).toHaveBeenCalledWith(
        expect.objectContaining({
          useResizeHandler: true,
        }),
        {},
      );
    });
  });

  it('disables modebar', () => {
    render(<GenericPlot chartData={[]} />);

    expect(AsyncPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        config: expect.objectContaining({
          displayModeBar: false,
        }),
      }),
      {},
    );
  });

  it('passes chart data to plot component', () => {
    render(<GenericPlot chartData={[{ x: 23 }, { x: 42 }]} />);

    expect(AsyncPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        data: [{ x: 23 }, { x: 42 }],
      }),
      {},
    );
  });

  it('extracts series color from context', () => {
    const lens = {
      colors: ColorMapper.builder().set('count()', '#783a8e').build(),
      setColor: jest.fn(),
    };
    const setChartColor = (chart: ChartConfig, colors: ColorMapper) => ({ marker: { color: colors.get(chart.name) } });
    render(
      <ChartColorContext.Provider value={lens}>
        <GenericPlot
          chartData={[
            { x: 23, name: 'count()' },
            { x: 42, name: 'sum(bytes)' },
          ]}
          setChartColor={setChartColor}
        />
      </ChartColorContext.Provider>,
    );

    expect(AsyncPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        data: [
          { 'marker': { 'color': '#783a8e' }, 'name': 'count()', 'outsidetextfont': { 'color': '#252D47' }, 'x': 23 },
          {
            'marker': { 'color': '#fd9e48' },
            'name': 'sum(bytes)',
            'outsidetextfont': { 'color': '#252D47' },
            'x': 42,
          },
        ],
      }),
      {},
    );
  });

  it('calls render completion callback after plotting', () => {
    asMock(AsyncPlot).mockImplementation(({ onAfterPlot }) => {
      onAfterPlot();

      return <span>Graph</span>;
    });

    const onRenderComplete = jest.fn();
    render(
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <GenericPlot
          chartData={[
            { x: 23, name: 'count()' },
            { x: 42, name: 'sum(bytes)' },
          ]}
        />
      </RenderCompletionCallback.Provider>,
    );

    expect(onRenderComplete).toHaveBeenCalled();
  });
});
