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
import 'helpers/mocking/react-dom_mock';
import { act } from 'react-dom/test-utils';

import TokenList from 'components/users/TokenList';

jest.mock('components/common/ClipboardButton', () => 'clipboard-button');

describe('<TokenList />', () => {
  const tokens = [
    { name: 'Acme', token: 'beef2001', id: 'abc1', last_access: '2020-12-08T16:46:00Z' },
    { name: 'Hamfred', token: 'beef2002', id: 'abc2', last_access: '1970-01-01T00:00:00.000Z' },
  ];

  it('should render with empty tokens', () => {
    const wrapper = mount(<TokenList tokens={[]} />);

    expect(wrapper).toExist();
  });

  it('should render with tokens', () => {
    const wrapper = mount(<TokenList tokens={tokens} />);

    expect(wrapper).toExist();
  });

  it('should add new token and display it', async () => {
    const createFn = jest.fn((tokenName) => {
      expect(tokenName).toEqual('hans');

      return Promise.resolve({ name: 'hans', token: 'beef2003', id: 'abc3', last_access: '1970-01-01T00:00:00.000Z' });
    });
    const wrapper = mount(<TokenList tokens={tokens}
                                     onCreate={createFn}
                                     onDelete={() => {}} />);

    wrapper.find('input#create-token-input').simulate('change', { target: { value: 'hans' } });

    await act(async () => {
      wrapper.find('form').at(0).simulate('submit');
    });

    wrapper.setProps({}); // Force re-render
    const tokenContainer = wrapper.find('pre').first();

    expect(createFn.mock.calls.length).toBe(1);
    expect(tokenContainer.props().children).toContain('beef2003');
  });

  it('should delete a token', async () => {
    const deleteFn = jest.fn((tokenId) => {
      expect(tokenId).toEqual('abc1');
    });
    const wrapper = mount(<TokenList tokens={tokens}
                                     onCreate={() => {}}
                                     onDelete={deleteFn} />);

    wrapper.find('button[children="Delete"]').at(0).simulate('click');

    expect(deleteFn.mock.calls.length).toBe(1);
  });

  it('show include token last access time', () => {
    const wrapper = mount(<TokenList tokens={tokens} />);

    expect(wrapper.find(`time[dateTime="${tokens[0].last_access}"]`)).toHaveLength(1);
    expect(wrapper.find('div[children="Never used"]')).toHaveLength(1);

    expect(wrapper).toExist();
  });
});
