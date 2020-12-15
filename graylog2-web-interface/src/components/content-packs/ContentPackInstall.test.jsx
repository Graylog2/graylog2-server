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

import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';

describe('<ContentPackInstall />', () => {
  const parameter = {
    title: 'A parameter',
    name: 'PARAM',
    type: 'string',
    default_value: 'parameter',
  };

  const entity = {
    type: {
      name: 'grok_pattern',
      version: '1',
    },
    data: {
      title: { '@type': 'string', '@value': 'franz' },
      descr: { '@type': 'string', '@value': 'hans' },
    },
  };

  const contentPack = ContentPack.builder()
    .id(1)
    .rev(2)
    .name('UFW Grok Patterns')
    .description('Grok Patterns to extract informations from UFW logfiles')
    .summary('This is a summary')
    .url('www.graylog.com')
    .vendor('graylog.com')
    .parameters([parameter])
    .entities([entity])
    .build();

  it('should render a install', () => {
    const wrapper = mount(<ContentPackInstall contentPack={contentPack} />);

    expect(wrapper).toExist();
  });

  it('should call install when called', () => {
    const installFn = jest.fn((id, rev, param) => {
      expect(id).toBe(1);
      expect(rev).toBe(2);
      expect(param).toEqual({ comment: 'Test', parameters: { PARAM: { '@type': 'string', '@value': 'parameter' } } });
    });

    const wrapper = mount(<ContentPackInstall contentPack={contentPack} onInstall={installFn} />);

    wrapper.find('input#comment').simulate('change', { target: { value: 'Test' } });
    wrapper.instance().onInstall();

    expect(installFn.mock.calls.length).toBe(1);
  });

  it('should not call install when parameter is missing', () => {
    const installFn = jest.fn();

    const wrapper = mount(<ContentPackInstall contentPack={contentPack} onInstall={installFn} />);

    wrapper.find('input').at(1).simulate('change', { target: { value: '' } });
    wrapper.instance().onInstall();

    expect(installFn.mock.calls.length).toBe(0);
  });
});
