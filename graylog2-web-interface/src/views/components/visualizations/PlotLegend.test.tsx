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
import { StoreMock as MockStore } from 'helpers/mocking';

import PlotLegend from 'views/components/visualizations/PlotLegend';
import ColorMapper from 'views/components/visualizations/ColorMapper';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import ChartColorContext from './ChartColorContext';

jest.mock('views/stores/CurrentViewStateStore', () => ({
  CurrentViewStateStore: MockStore(
    ['getInitialState', () => {
      return {
        activeQuery: 'active-query-id',
      };
    },
    ],
  ),
}));

const colors = ColorMapper.create();
const setColor = jest.fn();
const chartData = [
  { name: 'name1' },
  { name: 'name2' },
  { name: 'name3' },
];
const columnPivots = [Pivot.create('field1', 'unknown')];
const config = AggregationWidgetConfig.builder().columnPivots(columnPivots).build();

// eslint-disable-next-line react/require-default-props
const SUT = ({ chartDataProp = chartData }: { chartDataProp?: Array<{ name: string }>}) => (
  <WidgetFocusContext.Provider value={{
    focusedWidget: undefined,
    setWidgetFocusing: jest.fn(),
    unsetWidgetFocusing: jest.fn(),
    unsetWidgetEditing: jest.fn(),
    setWidgetEditing: jest.fn(),
  }}>
    <ChartColorContext.Provider value={{ colors, setColor }}>
      <PlotLegend config={config} chartData={chartDataProp}>
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

  it('should set a color when clicking on the color hing', async () => {
    render(<SUT />);
    const colorHints = await screen.findAllByLabelText('Color Hint');
    fireEvent.click(colorHints[0]);

    screen.getByText('Configuration for name1');
    const color = screen.getByTitle('#b71c1c');
    fireEvent.click(color);

    waitFor(() => {
      expect(setColor).toBeCalledWith('name1', '#b71c1c');
    });
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

  it('should hide with a single values', async () => {
    render(<SUT chartDataProp={[{ name: 'name1' }]} />);

    expect(screen.queryByText('name1')).not.toBeInTheDocument();
  });
});
