import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';

describe('<ContentPackEntitiesList />', () => {
  it('should render with empty entities', () => {
    const contentPack = { entities: [] };
    const wrapper = renderer.create(<ContentPackEntitiesList contentPack={contentPack} readOnly />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with entities and parameters without readOnly', () => {
    const entity = {
      id: '111-beef',
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
    const contentPack = {
      entities: [entity],
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      }],
    };
    const appliedParameter = { '111-beef': [{ configKey: 'title', paramName: 'A parameter name' }] };

    const wrapper = renderer.create(<ContentPackEntitiesList contentPack={contentPack}
                                                             appliedParameter={appliedParameter} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
