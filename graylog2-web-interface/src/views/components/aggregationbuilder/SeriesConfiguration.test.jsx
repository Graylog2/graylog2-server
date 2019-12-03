import React from 'react';
import renderer from 'react-test-renderer';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';

import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import SeriesConfiguration from './SeriesConfiguration';

describe('SeriesConfiguration', () => {
  const createNewSeries = (series = Series.forFunction('count()'), name) => {
    const newConfig = series.config.toBuilder().name(name).build();
    return series.toBuilder().config(newConfig).build();
  };

  it('renders the configuration dialog', () => {
    const wrapper = renderer.create(<SeriesConfiguration series={createNewSeries()} onClose={() => {}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('renders an input to change the series name', () => {
    const wrapper = mount(<SeriesConfiguration series={createNewSeries()} onClose={() => {}} />);
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
    const series = createNewSeries();
    const wrapper = mount(<SeriesConfiguration series={series} onClose={onClose} />);

    const submit = wrapper.find('button');

    submit.simulate('click');

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onClose).toHaveBeenCalledWith(series);
  });

  it('returns changed name upon submit', () => {
    const onClose = jest.fn();
    const series = createNewSeries();
    const wrapper = mount(<SeriesConfiguration series={series} onClose={onClose} />);
    const input = wrapper.find('input');
    const submit = wrapper.find('button');

    expect(input).toHaveProp('value', 'count()');

    input.simulate('change', { target: { value: 'Some other value' } });

    submit.simulate('click');

    const newSeries = createNewSeries(series, 'Some other value');
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onClose).toHaveBeenCalledWith(newSeries);
  });

  it('returns original metric function name upon submit, when no name is defined', () => {
    const onClose = jest.fn();
    const series = createNewSeries(undefined, 'Some other value');
    const wrapper = mount(<SeriesConfiguration series={series} onClose={onClose} />);
    const input = wrapper.find('input');
    const submit = wrapper.find('button');

    expect(input).toHaveProp('value', 'Some other value');

    input.simulate('change', { target: { value: '' } });

    submit.simulate('click');

    const newSeries = createNewSeries(series, 'count()');
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onClose).toHaveBeenCalledWith(newSeries);
  });
});
