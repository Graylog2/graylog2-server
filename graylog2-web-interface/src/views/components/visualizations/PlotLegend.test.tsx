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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';

import PlotLegend from 'views/components/visualizations/PlotLegend';
import ColorMapper from 'views/components/visualizations/ColorMapper';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import Series from 'views/logic/aggregationbuilder/Series';

import ChartColorContext from './ChartColorContext';

jest.mock('views/logic/queries/useCurrentQueryId', () => () => 'active-query-id');
jest.mock('stores/useAppDispatch');

const colors = ColorMapper.create();
const setColor = jest.fn();
const chartData = [
  { name: 'name1' },
  { name: 'name2' },
  { name: 'name3' },
];
const columnPivots = [Pivot.create(['field1'], 'unknown')];
const config = AggregationWidgetConfig.builder().series([Series.forFunction('count')]).columnPivots(columnPivots).build();

// eslint-disable-next-line react/require-default-props
const SUT = ({ chartDataProp = chartData, plotConfig = config, neverHide = false }: { chartDataProp?: Array<{ name: string, }>, plotConfig?: AggregationWidgetConfig, neverHide?: boolean }) => (
  <WidgetFocusContext.Provider value={{
    focusedWidget: undefined,
    setWidgetFocusing: jest.fn(),
    unsetWidgetFocusing: jest.fn(),
    unsetWidgetEditing: jest.fn(),
    setWidgetEditing: jest.fn(),
  }}>
    <ChartColorContext.Provider value={{ colors, setColor }}>
      <PlotLegend config={plotConfig} chartData={chartDataProp} neverHide={neverHide}>
        <div>Plot</div>
      </PlotLegend>
    </ChartColorContext.Provider>
  </WidgetFocusContext.Provider>
);

describe('PlotLegend', () => {
  it('should render the plot legend', async () => {
    render(<SUT />);
    await screen.findByText('name1');
    await screen.findByText('name2');
    await screen.findByText('name3');
  });

  it('should render the color hint', async () => {
    render(<SUT />);
    await screen.findAllByLabelText('Color Hint');
  });

  it('should set a color when clicking on the color hint', async () => {
    render(<SUT />);
    const colorHints = await screen.findAllByLabelText('Color Hint');
    fireEvent.click(colorHints[0]);

    screen.getByText('Configuration for name1');
    const color = screen.getByTitle('#b71c1c');
    fireEvent.click(color);

    await waitFor(() => expect(setColor).toHaveBeenCalledWith('name1', '#b71c1c'));
  });

  it('should open the value context menu', async () => {
    render(<SUT />);

    const value = await screen.findByText('name1');
    fireEvent.click(value);

    await screen.findByText('Actions');
  });

  it('should render with a lot of values', async () => {
    const charDataProp = [1, 2, 3, 4, 5, 6, 7, 8, 10, 11].map((i) => ({ name: `name${i}` }));
    render(<SUT chartDataProp={charDataProp} />);
    await screen.findByText('name1');
    await screen.findByText('name11');
  });

  it('should hide with a single value', async () => {
    const plotConfig = AggregationWidgetConfig.builder().series([Series.forFunction('count')]).build();
    render(<SUT chartDataProp={[{ name: 'name1' }]} plotConfig={plotConfig} />);

    expect(screen.queryByText('name1')).not.toBeInTheDocument();
  });

  it('should not add value action menu for series', async () => {
    render(<SUT chartDataProp={[{ name: 'name1' }, { name: 'count' }]} />);

    const value = await screen.findByText('count');
    fireEvent.click(value);

    expect(screen.queryByText('Actions')).not.toBeInTheDocument();
  });

  it('should not hide with a single value if configured', async () => {
    const plotConfig = AggregationWidgetConfig.builder().series([Series.forFunction('count')]).build();
    render(<SUT chartDataProp={[{ name: 'name1' }]} plotConfig={plotConfig} neverHide />);

    await screen.findByText('name1');
  });
});
