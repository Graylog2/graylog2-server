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
import React from 'react';
import { mount } from 'wrappedEnzyme';

import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

import SeriesConfiguration from './SeriesConfiguration';

describe('SeriesConfiguration', () => {
  const createNewSeries = (series = Series.forFunction('count()'), name) => {
    const newConfig = series.config.toBuilder().name(name).build();

    return series.toBuilder().config(newConfig).build();
  };

  it('renders the configuration dialog', () => {
    const wrapper = mount(<SeriesConfiguration series={createNewSeries()} onClose={() => {}} />);

    expect(wrapper).toExist();
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

  it('prevents entering a duplicate series name', () => {
    const onClose = jest.fn();
    const series = createNewSeries();
    const wrapper = mount(<SeriesConfiguration series={series} onClose={onClose} usedNames={['Already Exists']} />);
    const input = wrapper.find('input');

    expect(input).toHaveProp('value', 'count()');

    input.simulate('change', { target: { value: 'Already Exists' } });

    wrapper.update();
    const submit = wrapper.find('button');

    expect(submit).toBeDisabled();
    expect(wrapper).toIncludeText('Name must be unique');
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
