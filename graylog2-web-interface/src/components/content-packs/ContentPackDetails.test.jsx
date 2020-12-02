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
import ContentPackDetails from 'components/content-packs/ContentPackDetails';

describe('<ContentPackDetails />', () => {
  it('should render with content pack', () => {
    const contentPack = ContentPack.builder()
      .id('1')
      .name('UFW Grok Patterns')
      .description('Grok Patterns to extract informations from UFW logfiles')
      .summary('This is a summary')
      .vendor('graylog.com')
      .url('http://www.graylog.com')
      .build();
    const wrapper = mount(<ContentPackDetails contentPack={contentPack} />);

    expect(wrapper.find('[href="http://www.graylog.com"]')).toHaveLength(1);
    expect(wrapper).toExist();
  });

  it('should render with content pack without a description', () => {
    const contentPack = {
      id: '1',
      title: 'UFW Grok Patterns',
      version: '1.0',
      states: ['installed', 'edited'],
      summary: 'This is a summary',
      vendor: 'graylog.com',
      url: 'http://www.graylog.com',
      parameters: [],
      entities: [],
    };
    const wrapper = mount(<ContentPackDetails contentPack={contentPack} />);

    expect(wrapper).toExist();
  });

  it('should not render an anchor if URL protocol is not accepted', () => {
    const contentPack = {
      id: '1',
      title: 'UFW Grok Patterns',
      version: '1.0',
      states: ['installed', 'edited'],
      summary: 'This is a summary',
      vendor: 'graylog.com',
      url: 'thisurlisgreat',
      parameters: [],
      entities: [],
    };
    const wrapper = mount(<ContentPackDetails contentPack={contentPack} />);

    expect(wrapper.find('[href="thisurlisgreat"]')).toHaveLength(0);
  });

  it('should sanitize generated HTML from Markdown', () => {
    const contentPack = {
      id: '1',
      title: 'UFW Grok Patterns',
      description: 'great content [click me](javascript:alert(123))',
      version: '1.0',
      states: ['installed', 'edited'],
      summary: 'This is a summary',
      vendor: 'graylog.com',
      url: '',
      parameters: [],
      entities: [],
    };
    const wrapper = mount(<ContentPackDetails contentPack={contentPack} />);
    const descriptionContainer = wrapper.find('[dangerouslySetInnerHTML]');

    expect(descriptionContainer).toHaveLength(1);
    // eslint-disable-next-line no-script-url
    expect(descriptionContainer.html()).not.toContain('javascript:alert(123)');
  });
});
