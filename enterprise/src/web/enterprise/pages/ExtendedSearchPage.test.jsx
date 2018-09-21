import React from 'react';
import { shallow } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import ExtendedSearchPage from './ExtendedSearchPage';

jest.mock('enterprise/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('enterprise/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('enterprise/stores/StreamsStore', () => ({ StreamsActions: { refresh: jest.fn() } }));
jest.mock('enterprise/components/common/WindowLeaveMessage', () => mockComponent('WindowLeaveMessage'));

describe('ExtendedSearchPage', () => {
  it('register a WindowLeaveMessage', () => {
    const wrapper = shallow(<ExtendedSearchPage />);

    expect(wrapper.find('WindowLeaveMessage')).toHaveLength(1);
  });

  it('passes the given route to the WindowLeaveMessage component', () => {
    const route = { path: '/foo' };
    const wrapper = shallow(<ExtendedSearchPage route={route} />);

    const windowLeaveMessage = wrapper.find('WindowLeaveMessage');
    expect(windowLeaveMessage).toHaveLength(1);
    expect(windowLeaveMessage).toHaveProp('route', route);
  });
});