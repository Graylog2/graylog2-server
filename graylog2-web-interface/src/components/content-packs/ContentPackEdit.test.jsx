import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';

jest.mock('react-overlays', () => ({
  // eslint-disable-next-line
  AutoAffix: ({ children }) => <div>{children}</div>,
}));


describe('<ContentPackEdit />', () => {
  const emptyContentPack = ContentPack.builder()
    .id('9950ba5a-0887-40a9-2b8f-8b50292cc7fb')
    .build();

  const enrichedEntity = {
    id: '111-beef',
    v: '1.0',
    type: 'dashboard',
    title: 'A good input',
    data: {
      name: 'Input',
      title: 'A good input',
      configuration: {
        listen_address: '1.2.3.4',
        port: '23',
      },
    },
  };
  const serverEntities = {
    dashboard: [{ id: '111-beef', type: 'dashboard', title: 'A good input' }],
  };
  const appliedParameter = { '111-beef': [{ configKey: 'configuration.port', paramName: 'PORT' }] };
  const selectedEntities = { dashboard: [{ id: '111-beef', type: 'dashbaord' }] };

  const parameter = { title: 'Port', name: 'PORT', type: 'integer', default_value: '23' };
  const filledContentPack = ContentPack.builder()
    .id('9950ba5a-0887-40a9-2b8f-8b50292cc7fb')
    .name('Content Pack the movie')
    .description('## No one dares')
    .summary('A old content pack')
    .vendor('Beinstein')
    .url('http://beinstein.com')
    .entities([enrichedEntity])
    .parameters([parameter])
    .build();

  it('should render spinner with no content pack', () => {
    const wrapper = mount(<ContentPackEdit />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render empty content pack for create', () => {
    const wrapper = mount(<ContentPackEdit contentPack={emptyContentPack}
                                           selectedEntities={{}}
                                           appliedParameter={{}}
                                           entityIndex={{}} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with content pack for edit', () => {
    const wrapper = mount(
      <ContentPackEdit contentPack={filledContentPack}
                       appliedParameter={appliedParameter}
                       entityIndex={serverEntities}
                       selectedEntities={selectedEntities} />,
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should create a new content pack', () => {
    const saveFn = jest.fn();
    const wrapper = mount(
      <ContentPackEdit contentPack={filledContentPack}
                       appliedParameter={appliedParameter}
                       entityIndex={serverEntities}
                       onSave={saveFn}
                       selectedEntities={selectedEntities} />,
    );
    wrapper.find('button[children="Next"]').simulate('click');
    expect(wrapper.find('h2[children="Parameters list"]').exists()).toBe(true);
    wrapper.find('button[children="Next"]').simulate('click');
    expect(wrapper.find('button[children="Create"]').exists()).toBe(true);
    wrapper.find('button[children="Create"]').simulate('click');
    expect(saveFn.mock.calls.length).toBe(1);
  });
});
