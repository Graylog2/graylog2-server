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
import * as URLUtils from 'util/URLUtils';
import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';
import ContentPackVersions from 'components/content-packs/ContentPackVersions';

describe('<ContentPackVersions />', () => {
  // TODO: Should be replaced with call to `jest.mock` instead.
  // eslint-disable-next-line import/no-named-as-default-member
  URLUtils.areCredentialsInURLSupported = jest.fn(() => { return false; });
  const contentPackRev = ContentPack.builder()
    .id('1')
    .name('UFW Grok Patterns')
    .description('Grok Patterns to extract informations from UFW logfiles')
    .summary('This is a summary')
    .vendor('graylog.com')
    .url('www.graylog.com');

  const contentPack = {
    1: contentPackRev.rev(1).build(),
    2: contentPackRev.rev(2).build(),
    3: contentPackRev.rev(3).build(),
    4: contentPackRev.rev(4).build(),
  };

  const contentPackRevision = new ContentPackRevisions(contentPack);

  it('should render with content pack versions', () => {
    const wrapper = mount(<ContentPackVersions contentPackRevisions={contentPackRevision} />);

    expect(wrapper).toExist();
  });

  it('should fire on change when clicked on a version', () => {
    const changeFn = jest.fn((version) => {
      expect(version).toEqual('1');
    });
    const wrapper = mount(<ContentPackVersions onChange={changeFn} contentPackRevisions={contentPackRevision} />);

    wrapper.find('input[value=1]').simulate('change', { target: { checked: true, value: '1' } });

    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should fire on delete when clicked on delete a version', () => {
    const deleteFn = jest.fn((version, revision) => {
      expect(version).toEqual('1');
      expect(revision).toEqual(1);
    });
    const wrapper = mount(<ContentPackVersions onDeletePack={deleteFn} contentPackRevisions={contentPackRevision} />);

    wrapper.find('a[children="Delete"]').at(0).simulate('click');

    expect(deleteFn.mock.calls.length).toBe(1);
  });
});
