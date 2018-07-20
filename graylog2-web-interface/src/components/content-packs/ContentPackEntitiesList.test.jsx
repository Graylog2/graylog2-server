import React from 'react';
import { mount } from 'enzyme';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';

describe('<ContentPackEntitiesList />', () => {
  const entity1 = {
    id: '111-beef',
    type: 'Input',
    v: '1.0',
    data: {
      name: { type: 'string', value: 'Input' },
      title: { type: 'string', value: 'A good input' },
      configuration: {
        listen_address: { type: 'string', value: '1.2.3.4' },
        port: { type: 'integer', value: '23' },
      },
    },
  };
  const entity2 = {
    id: '121-beef',
    type: 'Input',
    v: '1.0',
    data: {
      name: { type: 'string', value: 'BadInput' },
      title: { type: 'string', value: 'A bad input' },
      configuration: {
        listen_address: { type: 'string', value: '1.2.3.4' },
        port: { type: 'integer', value: '22' },
      },
    },
  };
  const contentPack = {
    entities: [entity1, entity2],
    parameters: [{
      name: 'A parameter name',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }],
  };

  it('should render with empty entities', () => {
    const emptyContentPack = { entities: [] };
    const wrapper = renderer.create(<ContentPackEntitiesList contentPack={emptyContentPack} readOnly />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with entities and parameters without readOnly', () => {
    const appliedParameter = { '111-beef': [{ configKey: 'title', paramName: 'A parameter name' }] };

    const wrapper = renderer.create(<ContentPackEntitiesList contentPack={contentPack}
                                                             appliedParameter={appliedParameter} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should filter entities', () => {
    const wrapper = mount(<ContentPackEntitiesList contentPack={contentPack} />);
    expect(wrapper.find("td[children='A good input']").exists()).toBe(true);
    wrapper.find('input').simulate('change', { target: { value: 'Bad' } });
    wrapper.find('form').simulate('submit');
    expect(wrapper.find("td[children='A good input']").exists()).toBe(false);
    wrapper.find("button[children='Reset']").simulate('click');
    expect(wrapper.find("td[children='A good input']").exists()).toBe(true);
  });
});
