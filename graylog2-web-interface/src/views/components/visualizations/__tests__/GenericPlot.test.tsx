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

import ColorMapper from 'views/components/visualizations/ColorMapper';
import EChart from 'views/components/visualizations/echarts/EChart';
import asMock from 'helpers/mocking/AsMock';

import ChartColorContext from '../ChartColorContext';
import type { ChartConfig } from '../GenericPlot';
import GenericPlot from '../GenericPlot';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

jest.mock('components/bootstrap/Popover');
jest.mock('views/components/visualizations/echarts/EChart', () => jest.fn(() => <div data-testid="echart" />));
jest.mock('components/common/ColorPicker', () => 'color-picker');

describe('GenericPlot', () => {
  it('renders EChart component', () => {
    render(<GenericPlot chartData={[]} />);

    expect(EChart).toHaveBeenCalled();
  });

  it('passes option to EChart', () => {
    render(<GenericPlot chartData={[{ x: [1, 2], y: [3, 4], type: 'scatter', name: 'test' }]} />);

    expect(EChart).toHaveBeenCalledWith(
      expect.objectContaining({
        option: expect.objectContaining({
          series: expect.any(Array),
        }),
      }),
      {},
    );
  });

  it('extracts series color from context', () => {
    const lens = {
      colors: ColorMapper.builder().set('count()', '#783a8e').build(),
      setColor: jest.fn(),
    };
    const setChartColor = (chart: ChartConfig, colors: ColorMapper) => ({
      marker: { color: colors.get(chart.name) },
    });
    render(
      <ChartColorContext.Provider value={lens}>
        <GenericPlot
          chartData={[
            { x: [23], y: [1], name: 'count()', type: 'scatter' },
            { x: [42], y: [2], name: 'sum(bytes)', type: 'scatter' },
          ]}
          setChartColor={setChartColor}
        />
      </ChartColorContext.Provider>,
    );

    expect(EChart).toHaveBeenCalled();
    const lastCall = asMock(EChart).mock.calls[asMock(EChart).mock.calls.length - 1];
    const option = lastCall[0].option;

    expect((option.series as Array<any>).length).toBe(2);
  });

  it('calls render completion callback on chart ready', () => {
    asMock(EChart).mockImplementation(({ onChartReady }) => {
      React.useEffect(() => {
        onChartReady?.({
          getDom: () => document.createElement('div'),
          getZr: () => ({}),
          resize: () => {},
          dispose: () => {},
          on: () => {},
          off: () => {},
          convertFromPixel: () => {},
          convertToPixel: () => {},
          getOption: () => ({}),
        });
      }, [onChartReady]);

      return <span>Chart</span>;
    });

    const onRenderComplete = jest.fn();
    render(
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <GenericPlot chartData={[]} />
      </RenderCompletionCallback.Provider>,
    );

    expect(onRenderComplete).toHaveBeenCalled();
  });
});
