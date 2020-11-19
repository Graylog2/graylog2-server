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
// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import Series from 'views/logic/aggregationbuilder/Series';
import SeriesParameterOptions from 'views/components/aggregationbuilder/SeriesParameterOptions';

import SeriesSelect from './SeriesSelect';

jest.mock('views/stores/AggregationFunctionsStore', () => ({ getInitialState: jest.fn(), listen: jest.fn() }));

jest.mock('views/components/aggregationbuilder/SeriesParameterOptions', () => ({
  parameterOptionsForType: jest.fn(),
}));

describe('SeriesSelect', () => {
  let suggester;

  beforeEach(() => {
    suggester = () => [];
    suggester.defaults = [];
  });

  it('renders with minimal props', () => {
    const wrapper = mount(<SeriesSelect series={[]} onChange={() => true} />);

    expect(wrapper).toExist();
  });

  it('renders given series', () => {
    const series = [Series.forFunction('count()'), Series.forFunction('avg(took_ms)')];
    const wrapper = mount(<SeriesSelect series={series} onChange={() => true} />);

    expect(wrapper.find('ConfigurableElement')).toHaveLength(2);

    const values = wrapper.find('ConfigurableElement');

    expect(values.at(0)).toIncludeText('count()');
    expect(values.at(1)).toIncludeText('avg(took_ms)');
  });

  it('opens menu when focussed, returning no results without suggester', () => {
    const wrapper = mount(<SeriesSelect series={[]} onChange={() => true} />);
    const input = wrapper.find('input');

    expect(wrapper).not.toIncludeText('0 results available.');

    input.simulate('focus');

    expect(wrapper).toIncludeText('0 results available.');
  });

  it('opens menu when focussed, returning no results for empty suggester defaults', () => {
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={() => true} />);
    const input = wrapper.find('input');

    expect(wrapper).not.toIncludeText('0 results available.');

    input.simulate('focus');

    expect(wrapper).toIncludeText('0 results available.');
  });

  it('opens menu when focussed, returning suggester defaults', () => {
    suggester.defaults = [{ label: 'Something', value: 'Something' }, { label: 'Anything', value: 'Anything' }];
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={() => true} />);
    const input = wrapper.find('input');

    expect(wrapper).not.toIncludeText('Something');
    expect(wrapper).not.toIncludeText('Anything');

    input.simulate('focus');

    expect(wrapper).not.toIncludeText('0 results available.');
    expect(wrapper).toIncludeText('2 results available.');
  });

  it('shows next suggestions when selecting incomplete suggestion', () => {
    suggester.defaults = [
      { value: 'func1', incomplete: true, label: 'func1', parameterNeeded: false },
      { label: 'Anything', value: 'Anything' },
    ];

    suggester.for = jest.fn(() => [
      { label: 'func1(foo)', value: 'func1(foo)' },
      { label: 'func1(bar)', value: 'func1(bar)' },
    ]);

    const onChange = jest.fn();
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={onChange} />);
    const input = wrapper.find('input');

    input.simulate('focus');

    const select = wrapper.find('Select').at(0);

    select.prop('onChange')([{ label: 'func1', value: 'func1', incomplete: true }]);

    expect(suggester.for).toHaveBeenCalledTimes(1);
    expect(suggester.for).toHaveBeenCalledWith('func1', undefined);
    expect(onChange).not.toHaveBeenCalled();
  });

  it('shows parameter suggestions when parameter is needed', () => {
    // eslint-disable-next-line import/no-named-as-default-member
    SeriesParameterOptions.parameterOptionsForType = jest.fn(() => [1, 2, 3]);

    suggester.defaults = [
      { value: 'func1', incomplete: true, parameterNeeded: false, label: 'func1' },
      { label: 'Anything', value: 'Anything' },
    ];

    suggester.for = jest.fn(() => [
      { label: 'func1(foo)', value: 'func1(foo)' },
      { label: 'func1(bar)', value: 'func1(bar)' },
    ]);

    const onChange = jest.fn();
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={onChange} />);
    const input = wrapper.find('input');

    input.simulate('focus');

    const select = wrapper.find('Select').at(0);

    select.prop('onChange')([{ label: 'func1', value: 'func1', incomplete: true, parameterNeeded: true }]);

    // eslint-disable-next-line import/no-named-as-default-member
    expect(SeriesParameterOptions.parameterOptionsForType).toHaveBeenCalledTimes(1);

    // eslint-disable-next-line import/no-named-as-default-member
    expect(SeriesParameterOptions.parameterOptionsForType).toHaveBeenCalledWith('func1');
    expect(onChange).not.toHaveBeenCalled();
  });

  it('unwraps value and returns it when final option was selected', () => {
    const onChange = jest.fn();
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={onChange} />);

    expect(onChange).not.toHaveBeenCalled();

    const select = wrapper.find('Select').at(0);

    select.prop('onChange')([{ label: 'func1(foo)', value: 'func1(foo)' }]);

    expect(onChange).toHaveBeenCalled();
    expect(onChange).toHaveBeenCalledWith(['func1(foo)']);
  });

  it('allows removing the last element', () => {
    const onChange = jest.fn(() => true);
    const wrapper = mount(<SeriesSelect series={[Series.forFunction('count()')]} suggester={suggester} onChange={onChange} />);
    const removeSeries = wrapper.find('div[children="×"]');

    removeSeries.simulate('click');

    expect(onChange).toHaveBeenCalledWith([]);
  });
});
