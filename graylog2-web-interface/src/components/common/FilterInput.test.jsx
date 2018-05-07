import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import FilterInput from 'components/common/FilterInput';

describe('<FilterInput />', () => {
  it('should render a filter input', () => {
    const wrapper = renderer.create(<FilterInput onChange={() => {}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a filter input with custom label', () => {
    const wrapper = renderer.create(<FilterInput
      onChange={() => {}}
      filterLabel="Search"
      labelClassName="col-sm-3"
      wrapperClassName="col-xs-1"
    />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should take filter input', () => {
    const changeFn = jest.fn((value) => {
      expect(value).toEqual('kaladin');
    });
    const wrapper = mount(<FilterInput onChange={changeFn} />)

    wrapper.find('input').simulate('change', { target: { value: 'kaladin' } });
    expect(changeFn.mock.calls.length).toBe(1);
  });
});
