import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackEdit from 'components/content-packs/ContentPackEdit';

jest.mock('react-overlays', () => ({
  // eslint-disable-next-line
  AutoAffix: ({ children }) => <div>{children}</div>,
}));


describe('<ContentPackEdit />', () => {
  const emptyContentPack = {
    v: 1,
    id: '9950ba5a-0887-40a9-2b8f-8b50292cc7fb',
    rev: 1,
    requires: [],
    parameters: [],
    entities: [],
    name: '',
    summary: '',
    description: '',
    vendor: '',
    url: '',
  };
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
  const filledContentPack = {
    v: 1,
    id: '9950ba5a-0887-40a9-2b8f-8b50292cc7fb',
    rev: 1,
    requires: [],
    parameters: [parameter],
    entities: [enrichedEntity],
    name: 'Content Pack the movie',
    summary: 'A old content pack',
    description: '## No one dares',
    vendor: 'Beinstein',
    url: 'http://beinstein.com',
  };

  it('should render spinner with no content pack', () => {
    const wrapper = renderer.create(<ContentPackEdit />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render empty content pack for create', () => {
    const wrapper = renderer.create(<ContentPackEdit contentPack={emptyContentPack}
                       selectedEntities={{}}
                       appliedParameter={{}}
                       entityIndex={{}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with content pack for edit', () => {
    const wrapper = renderer.create(
      <ContentPackEdit contentPack={filledContentPack}
      appliedParameter={appliedParameter}
      entityIndex={serverEntities}
      selectedEntities={selectedEntities} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should create a new content pack', () => {
    const saveFn = jest.fn();
    const wrapper = mount(
      <ContentPackEdit contentPack={filledContentPack}
                       appliedParameter={appliedParameter}
                       entityIndex={serverEntities}
                       onSave={saveFn}
                       selectedEntities={selectedEntities} />);
    wrapper.find('button[children="Next"]').simulate('click');
    expect(wrapper.find('button[children="Create parameter"]').exists()).toBe(true);
    wrapper.find('button[children="Next"]').simulate('click');
    expect(wrapper.find('button[children="Create"]').exists()).toBe(true);
    wrapper.find('button[children="Create"]').simulate('click');
    expect(saveFn.mock.calls.length).toBe(1);
  });
});
