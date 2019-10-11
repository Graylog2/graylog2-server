import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackParameters from 'components/content-packs/ContentPackParameters';
import ContentPack from 'logic/content-packs/ContentPack';

describe('<ContentPackParameters />', () => {
  it('should render with empty parameters', () => {
    const contentPack = {
      parameters: [],
      entities: [],
    };
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} appliedParameter={{}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a parameter', () => {
    const entity = {
      id: '111-beef',
      v: '1.0',
      type: {
        name: 'input',
        version: '1',
      },
      data: {
        name: { '@type': 'string', '@value': 'Input' },
        title: { '@type': 'string', '@value': 'A good input' },
        configuration: {
          listen_address: { '@type': 'string', '@value': '1.2.3.4' },
          port: { '@type': 'integer', '@value': '23' },
        },
      },
    };
    const contentPack = ContentPack.builder()
      .parameters(
        [{
          name: 'A parameter name',
          title: 'A parameter title',
          description: 'A parameter descriptions',
          type: 'string',
          default_value: 'test',
        }],
      )
      .entities([entity])
      .build();
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} appliedParameter={{}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
