import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackInstallEntityList from './ContentPackInstallEntityList';

describe('<ContentPackInstallEntityList />', () => {
  const entities = [
    { id: '5bdc4f4b3d27461ce46816cf', type: { name: 'lookup_cache', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca80', title: 'Threat Intel Uncached Adapters' },
    { id: '5bdc4f4b3d27461ce46816d4', type: { name: 'lookup_table', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca8c', title: 'Tor Exit Node List' },
    { id: '5bdc4f4b3d27461ce46816d1', type: { name: 'lookup_adapter', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca87', title: 'Tor Exit Node' },
  ];

  it('should render without entities', () => {
    const wrapper = renderer.create(<ContentPackInstallEntityList />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with entities', () => {
    const wrapper = renderer.create(<ContentPackInstallEntityList entities={entities} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with empty entities', () => {
    const wrapper = renderer.create(<ContentPackInstallEntityList entities={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with uninstall and entities', () => {
    const wrapper = renderer.create(<ContentPackInstallEntityList uninstall entities={entities} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
