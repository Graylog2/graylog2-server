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

import TokenList from 'components/users/TokenList';

jest.mock('components/common/ClipboardButton', () => 'clipboard-button');

describe('<TokenList />', () => {
  const tokens = [
    { name: 'Acme', token: 'beef2001', id: 'abc1' },
    { name: 'Hamfred', token: 'beef2002', id: 'abc2' },
  ];

  it('should render with empty tokens', () => {
    const wrapper = mount(<TokenList tokens={[]} />);

    expect(wrapper).toExist();
  });

  it('should render with tokens', () => {
    const wrapper = mount(<TokenList tokens={tokens} />);

    expect(wrapper).toExist();
  });

  it('should add new token and delete existing ones', () => {
    const createFn = jest.fn((tokenName) => {
      expect(tokenName).toEqual('hans');
    });
    const deleteFn = jest.fn((tokenId) => {
      expect(tokenId).toEqual('abc1');
    });
    const wrapper = mount(<TokenList tokens={tokens}
                                     onCreate={createFn}
                                     onDelete={deleteFn} />);

    wrapper.find('input#create-token-input').simulate('change', { target: { value: 'hans' } });
    wrapper.find('form').at(0).simulate('submit');

    expect(createFn.mock.calls.length).toBe(1);

    wrapper.find('button[children="Delete"]').at(0).simulate('click');

    expect(createFn.mock.calls.length).toBe(1);
  });

  it('should display tokens if "Hide tokens" was unchecked', () => {
    const wrapper = mount(<TokenList tokens={tokens} />);

    expect(wrapper.find('pre[children="beef2001"]').length).toEqual(0);

    wrapper.find('input#hide-tokens').simulate('change', { target: { checked: false } });

    expect(wrapper.find('pre[children="beef2001"]').length).toEqual(1);
  });
});
