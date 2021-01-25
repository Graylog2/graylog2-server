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
import { mount, shallow } from 'wrappedEnzyme';

import 'helpers/mocking/react-dom_mock';
import URLUtils from 'util/URLUtils';
import ContentPacksList from 'components/content-packs/ContentPacksList';

describe('<ContentPacksList />', () => {
  // TODO: Should be replaced with call to `jest.mock` instead.
  // eslint-disable-next-line import/no-named-as-default-member
  URLUtils.areCredentialsInURLSupported = jest.fn(() => { return false; });

  const contentPacks = [
    { id: '1', rev: 1, title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0' },
    { id: '2', rev: 1, title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1' },
    { id: '3', rev: 1, title: 'Backup Content Pack', summary: '', version: '3.0' },
    { id: '4', rev: 1, title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4' },
    { id: '5', rev: 1, title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0' },
    { id: '6', rev: 1, title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0' },
    { id: '7', rev: 1, title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1' },
    { id: '8', rev: 1, title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'] },
    { id: '9', rev: 1, title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4' },
    { id: '10', rev: 1, title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0' },
    { id: '11', rev: 1, title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0' },
    { id: '12', rev: 1, title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1' },
    { id: '13', rev: 1, title: 'Backup Content Pack', summary: '', version: '3.0' },
    { id: '14', rev: 1, title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4' },
    { id: '15', rev: 1, title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0' },
  ];

  it('should render with empty content packs', () => {
    const wrapper = mount(<ContentPacksList contentPacks={[]} />);

    expect(wrapper).toExist();
  });

  it('should render with content packs', () => {
    const metadata = {
      1: { 1: { installation_count: 1 } },
      2: { 5: { installation_count: 2 } },
    };
    const wrapper = mount(<ContentPacksList contentPacks={contentPacks} contentPackMetadata={metadata} />);

    expect(wrapper).toExist();
  });

  it('should do pagination', () => {
    const NEXT_PAGE = 2;
    const wrapper = shallow(<ContentPacksList contentPacks={contentPacks} />);
    const onChangePageSpy = jest.spyOn(wrapper.instance(), '_onChangePage');
    const beforeFilter = wrapper.find('.content-packs-summary').length;

    expect(beforeFilter).toBe(10);

    wrapper.instance()._onChangePage(NEXT_PAGE);

    const afterFilter = wrapper.find('.content-packs-summary').length;

    expect(onChangePageSpy).toHaveBeenCalledWith(NEXT_PAGE);
    expect(afterFilter).toBe(5);
  });

  it('should delete a content pack', () => {
    const deleteFn = jest.fn((token) => {
      expect(token).toEqual('1');
    });
    const wrapper = mount(<ContentPacksList contentPacks={contentPacks} onDeletePack={deleteFn} />);

    wrapper.find('a[children="Delete All Versions"]').at(0).simulate('click');

    expect(deleteFn.mock.calls.length).toBe(1);
  });
});
