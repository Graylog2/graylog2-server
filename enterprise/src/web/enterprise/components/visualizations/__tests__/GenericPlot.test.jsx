import React from 'react';
import { mount } from 'enzyme';

import GenericPlot from '../GenericPlot';

describe('GenericPlot', () => {
  describe('adds onRelayout handler', () => {
    it('calling onZoom prop if axis have changed', () => {
      const onZoom = jest.fn();
      const wrapper = mount(<GenericPlot chartData={[]} onZoom={onZoom} />);

      const plot = wrapper.find('PlotlyComponent');
      const onRelayout = plot.get(0).props.onRelayout;

      onRelayout({ 'xaxis.range[0]': 'foo', 'xaxis.range[1]': 'bar' });

      expect(onZoom).toHaveBeenCalled();
      expect(onZoom).toHaveBeenCalledWith('foo', 'bar');
    });

    it('not calling onZoom prop if axis have not changed', () => {
      const onZoom = jest.fn();
      const wrapper = mount(<GenericPlot chartData={[]} onZoom={onZoom} />);

      const plot = wrapper.find('PlotlyComponent');
      const onRelayout = plot.get(0).props.onRelayout;

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
});