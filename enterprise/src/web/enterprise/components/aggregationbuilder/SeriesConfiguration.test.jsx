import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';

import Series from 'enterprise/logic/aggregationbuilder/Series';
import SeriesConfig from 'enterprise/logic/aggregationbuilder/SeriesConfig';
import SeriesConfiguration from './SeriesConfiguration';

describe('SeriesConfiguration', () => {
  it('renders the configuration dialog', () => {
    const wrapper = renderer.create(<SeriesConfiguration series={Series.forFunction('count()')} onClose={() => {}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('renders an input to change the series name', () => {
    const wrapper = mount(<SeriesConfiguration series={Series.forFunction('count()')} onClose={() => {}} />);
    expect(wrapper.find('input')).toHaveProp('value', 'count()');
  });

  it('uses the series\' effective value for the input', () => {
    const series = Series.forFunction('avg(took_ms)')
      .toBuilder()
      .config(SeriesConfig.empty().toBuilder().name('Average Request Time').build())
      .build();
    const wrapper = mount(<SeriesConfiguration series={series} onClose={() => {}} />);
    expect(wrapper.find('input')).toHaveProp('value', 'Average Request Time');
  });

  it('submit button calls onClose callback', () => {
    const onClose = jest.fn();
    const series = Series.forFunction('count()');
    const wrapper = mount(<SeriesConfiguration series={series} onClose={onClose} />);

    const submit = wrapper.find('button');

    submit.simulate('click');

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onClose).toHaveBeenCalledWith(series);
  });

  it('returns changed name upon submit', () => {
    const onClose = jest.fn();
    const series = Series.forFunction('count()');
    const wrapper = mount(<SeriesConfiguration series={series} onClose={onClose} />);
    const input = wrapper.find('input');
    expect(input).toHaveProp('value', 'count()');
    const submit = wrapper.find('button');

    input.simulate('change', { target: { value: 'Some other value' } });

    submit.simulate('click');

    const newSeries = series.toBuilder()
      .config(series.config.toBuilder().name('Some other value').build())
      .build();
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onClose).toHaveBeenCalledWith(newSeries);
  });
});
