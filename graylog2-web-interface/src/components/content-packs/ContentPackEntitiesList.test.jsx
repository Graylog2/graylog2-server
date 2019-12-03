import React from 'react';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';
import Entity from 'logic/content-packs/Entity';

describe('<ContentPackEntitiesList />', () => {
  const parameter = {
    title: 'A parameter name',
    name: 'TITLE',
    description: 'A parameter descriptions',
    type: 'string',
    default_value: 'test',
  };

  const entity1 = Entity.builder()
    .id('111-beef')
    .type({ name: 'Input', version: '1' })
    .v('1.0')
    .data({
      title: { '@type': 'parameter', '@value': 'TITLE' },
      configuration: {
        listen_address: { '@type': 'string', '@value': '1.2.3.4' },
        port: { '@type': 'integer', '@value': '23' },
      },
    })
    .parameters([parameter])
    .build();

  const entity2 = Entity.builder()
    .id('121-beef')
    .type({ name: 'Input', version: '1' })
    .v('1.0')
    .data({
      name: { '@type': 'string', '@value': 'BadInput' },
      title: { '@type': 'string', '@value': 'A bad input' },
      configuration: {
        listen_address: { '@type': 'string', '@value': '1.2.3.4' },
        port: { '@type': 'integer', '@value': '22' },
      },
    })
    .fromServer(true)
    .build();

  const contentPack = ContentPack.builder()
    .entities([entity1, entity2])
    .parameters([parameter])
    .build();

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
    expect(wrapper.find("td[children='test']").exists()).toBe(true);
    wrapper.find('input').simulate('change', { target: { value: 'Bad' } });
    wrapper.find('form').simulate('submit');
    expect(wrapper.find("td[children='test']").exists()).toBe(false);
    wrapper.find("button[children='Reset']").simulate('click');
    expect(wrapper.find("td[children='test']").exists()).toBe(true);
  });
});
