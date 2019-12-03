import React from 'react';
import { mount } from 'theme/enzymeWithTheme';
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
      .url('www.graylog.com')
      .build();
    const wrapper = mount(<ContentPackDetails contentPack={contentPack} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with content pack without a description', () => {
    const contentPack = {
      id: '1',
      title: 'UFW Grok Patterns',
      version: '1.0',
      states: ['installed', 'edited'],
      summary: 'This is a summary',
      vendor: 'graylog.com',
      url: 'www.graylog.com',
      parameters: [],
      entities: [],
    };
    const wrapper = mount(<ContentPackDetails contentPack={contentPack} />);
    expect(wrapper).toMatchSnapshot();
  });
});
