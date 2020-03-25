import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackInstallEntityList from './ContentPackInstallEntityList';

describe('<ContentPackInstallEntityList />', () => {
  const entities = [
    { id: '5bdc4f4b3d27461ce46816cf', type: { name: 'lookup_cache', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca80', title: 'Threat Intel Uncached Adapters' },
    { id: '5bdc4f4b3d27461ce46816d4', type: { name: 'lookup_table', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca8c', title: 'Tor Exit Node List' },
    { id: '5bdc4f4b3d27461ce46816d1', type: { name: 'lookup_adapter', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca87', title: 'Tor Exit Node' },
  ];

  it('should render without entities', () => {
    const wrapper = mount(<ContentPackInstallEntityList />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with entities', () => {
    const wrapper = mount(<ContentPackInstallEntityList entities={entities} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with empty entities', () => {
    const wrapper = mount(<ContentPackInstallEntityList entities={[]} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with uninstall and entities', () => {
    const wrapper = mount(<ContentPackInstallEntityList uninstall entities={entities} />);
    expect(wrapper).toMatchSnapshot();
  });
});
