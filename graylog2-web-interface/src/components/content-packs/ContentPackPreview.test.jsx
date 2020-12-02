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
import ContentPackPreview from 'components/content-packs/ContentPackPreview';

describe('<ContentPackPreview />', () => {
  it('should render with empty content pack', () => {
    const contentPack = ContentPack.builder().id('dead-beef').build();
    const wrapper = mount(<ContentPackPreview contentPack={contentPack} />);

    expect(wrapper).toExist();
  });

  it('should render with filled content pack', () => {
    const contentPack = ContentPack.builder()
      .id('dead-beef')
      .name('name')
      .summary('summary')
      .description('descr')
      .vendor('vendor')
      .url('http://example.com')
      .build();

    const wrapper = mount(<ContentPackPreview contentPack={contentPack} />);

    expect(wrapper).toExist();
  });

  it('should call onSave when creating a content pack', () => {
    const contentPack = ContentPack.builder()
      .id('dead-beef')
      .name('name')
      .summary('summary')
      .description('descr')
      .vendor('vendor')
      .url('http://example.com')
      .build();

    const onSave = jest.fn();
    const wrapper = mount(<ContentPackPreview contentPack={contentPack} onSave={onSave} />);

    wrapper.find('button#create').simulate('click');

    expect(onSave.mock.calls.length).toBe(1);
  });
});
