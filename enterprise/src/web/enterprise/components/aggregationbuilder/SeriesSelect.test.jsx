import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';

import Series from 'enterprise/logic/aggregationbuilder/Series';

import SeriesSelect from './SeriesSelect';

jest.mock('enterprise/stores/AggregationFunctionsStore', () => ({ getInitialState: jest.fn(), listen: jest.fn() }));

describe('SeriesSelect', () => {
  it('renders with minimal props', () => {
    const wrapper = renderer.create(<SeriesSelect series={[]} onChange={() => {}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('renders given series', () => {
    const series = [Series.forFunction('count()'), Series.forFunction('avg(took_ms)')];
    const wrapper = mount(<SeriesSelect series={series} onChange={() => {}} />);

    expect(wrapper.find('ConfigurableElement')).toHaveLength(2);
    const values = wrapper.find('ConfigurableElement');
    expect(values.at(0)).toIncludeText('count()');
    expect(values.at(1)).toIncludeText('avg(took_ms)');
  });

  it('opens menu when focussed, returning no results without suggester', () => {
    const wrapper = mount(<SeriesSelect series={[]} onChange={() => {}} />);
    const input = wrapper.find('input');
    expect(wrapper).not.toIncludeText('0 results available.');

    input.simulate('focus');

    expect(wrapper).toIncludeText('0 results available.');
  });

  it('opens menu when focussed, returning no results for empty suggester defaults', () => {
    const suggester = { defaults: [] };
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={() => {}} />);
    const input = wrapper.find('input');
    expect(wrapper).not.toIncludeText('0 results available.');

    input.simulate('focus');

    expect(wrapper).toIncludeText('0 results available.');
  });

  it('opens menu when focussed, returning suggester defaults', () => {
    const suggester = { defaults: [{ label: 'Something', value: 'Something' }, { label: 'Anything', value: 'Anything' }] };
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={() => {}} />);
    const input = wrapper.find('input');
    expect(wrapper).not.toIncludeText('Something');
    expect(wrapper).not.toIncludeText('Anything');

    input.simulate('focus');

    expect(wrapper).not.toIncludeText('0 results available.');
    expect(wrapper).toIncludeText('2 results available.');
  });

  it('shows next suggestions when selecting incomplete suggestion', () => {
    const suggester = {
      defaults: [{ label: 'func1', value: 'func1', incomplete: true }, { label: 'Anything', value: 'Anything' }],
      for: jest.fn(() => [
        { label: 'func1(foo)', value: 'func1(foo)' },
        { label: 'func1(bar)', value: 'func1(bar)' },
      ]),
    };
    const onChange = jest.fn();
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={onChange} />);
    const input = wrapper.find('input');
    input.simulate('focus');

    const select = wrapper.find('Select').at(0);
    select.prop('onChange')([{ label: 'func1', value: 'func1', incomplete: true }]);

    expect(suggester.for).toHaveBeenCalledTimes(1);
    expect(suggester.for).toHaveBeenCalledWith('func1');
    expect(onChange).not.toHaveBeenCalled();
  });

  it('unwraps value and returns it when final option was selected', () => {
    const suggester = {};
    const onChange = jest.fn();
    const wrapper = mount(<SeriesSelect series={[]} suggester={suggester} onChange={onChange} />);

    expect(onChange).not.toHaveBeenCalled();

    const select = wrapper.find('Select').at(0);
    select.prop('onChange')([{ label: 'func1(foo)', value: 'func1(foo)' }]);

    expect(onChange).toHaveBeenCalled();
    expect(onChange).toHaveBeenCalledWith(['func1(foo)']);
  });
});
