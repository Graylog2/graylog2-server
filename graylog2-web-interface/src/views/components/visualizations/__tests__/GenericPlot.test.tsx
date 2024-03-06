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
import type { HTMLAttributes } from 'enzyme';
import { mount } from 'wrappedEnzyme';
import type { PlotParams } from 'react-plotly.js';

import ColorMapper from 'views/components/visualizations/ColorMapper';

import ChartColorContext from '../ChartColorContext';
import type { ChartConfig } from '../GenericPlot';
import GenericPlot from '../GenericPlot';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

// We need to mock the Popover, because it implements the GraylogThemeProvider which does not render its children
// without CurrentUser & UserPreferences
jest.mock('components/bootstrap/Popover');

// eslint-disable-next-line global-require
jest.mock('views/components/visualizations/plotly/AsyncPlot', () => require('views/components/visualizations/plotly/Plot').default);
jest.mock('components/common/ColorPicker', () => 'color-picker');

describe('GenericPlot', () => {
  describe('adds onRelayout handler', () => {
    it('calling onZoom prop if axis have changed', () => {
      const onZoom = jest.fn();
      const wrapper = mount(<GenericPlot chartData={[]} onZoom={onZoom} />);

      const plot = wrapper.find('PlotlyComponent');
      const { onRelayout } = plot.get(0).props;

      onRelayout({ 'xaxis.range[0]': 'foo', 'xaxis.range[1]': 'bar' });

      expect(onZoom).toHaveBeenCalled();
      expect(onZoom).toHaveBeenCalledWith('foo', 'bar');
    });

    it('not calling onZoom prop if axis have not changed', () => {
      const onZoom = jest.fn();
      const wrapper = mount(<GenericPlot chartData={[]} onZoom={onZoom} />);

      const plot = wrapper.find('PlotlyComponent');
      const { onRelayout } = plot.get(0).props;

      onRelayout({ autosize: true });

      expect(onZoom).not.toHaveBeenCalled();
    });
  });

  describe('layout handling', () => {
    it('configures legend to be displayed horizontally', () => {
      const wrapper = mount(<GenericPlot chartData={[]} />);

      const plot = wrapper.find('PlotlyComponent');
      const generatedLayout = plot.get(0).props.layout;

      expect(generatedLayout.legend.orientation).toEqual('h');
    });

    it('merges in passed layout property', () => {
      const layout = { customProperty: 42 };
      const wrapper = mount(<GenericPlot chartData={[]} layout={layout} />);

      const plot = wrapper.find('PlotlyComponent');
      const generatedLayout = plot.get(0).props.layout;

      expect(generatedLayout.customProperty).toEqual(42);
      expect(generatedLayout.autosize).toEqual(true);
    });

    it('configures resize handler to be used', () => {
      const wrapper = mount(<GenericPlot chartData={[]} />);

      const plot = wrapper.find('PlotlyComponent');

      expect(plot).toHaveProp('useResizeHandler', true);
    });
  });

  it('disables modebar', () => {
    const wrapper = mount(<GenericPlot chartData={[]} />);

    const plot = wrapper.find('PlotlyComponent');
    const generatedConfig = plot.get(0).props.config;

    expect(generatedConfig.displayModeBar).toEqual(false);
  });

  it('passes chart data to plot component', () => {
    const wrapper = mount(<GenericPlot chartData={[{ x: 23 }, { x: 42 }]} />);

    const plot = wrapper.find('PlotlyComponent');

    expect(plot).toHaveProp('data', [{ x: 23 }, { x: 42 }]);
  });

  it('extracts series color from context', () => {
    const lens = {
      colors: ColorMapper.builder().set('count()', '#783a8e').build(),
      setColor: jest.fn(),
    };
    const setChartColor = (chart, colors) => ({ marker: { color: colors.get(chart.name) } });
    const wrapper = mount((
      <ChartColorContext.Provider value={lens}>
        <GenericPlot chartData={[{ x: 23, name: 'count()' }, { x: 42, name: 'sum(bytes)' }]} setChartColor={setChartColor} />
      </ChartColorContext.Provider>
    ));

    const { data: newChartData } = wrapper.find('PlotlyComponent').props() as HTMLAttributes & { data: ChartConfig[] };

    expect(newChartData.find((chart) => chart.name === 'count()').marker.color).toEqual('#783a8e');
    expect(newChartData.find((chart) => chart.name === 'sum(bytes)').marker.color).toEqual('#fd9e48');
  });

  it('calls render completion callback after plotting', () => {
    const onRenderComplete = jest.fn();
    const wrapper = mount((
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <GenericPlot chartData={[{ x: 23, name: 'count()' }, { x: 42, name: 'sum(bytes)' }]} />
      </RenderCompletionCallback.Provider>
    ));
    const { onAfterPlot } = wrapper.find('PlotlyComponent').props() as HTMLAttributes & PlotParams;

    onAfterPlot();

    expect(onRenderComplete).toHaveBeenCalled();
  });
});
