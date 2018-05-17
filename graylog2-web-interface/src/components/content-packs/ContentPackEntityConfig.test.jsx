import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackEntityConfig from 'components/content-packs/ContentPackEntityConfig';

describe('<ContentPackEntityConfig />', () => {
  it('should render with a entity', () => {
    const entity = {
      data: {
        title: { type: 'string', value: 'franz' },
        descr: { type: 'string', value: 'hans' },
      },
    };
    const appliedParameter = [{ configKey: 'descr', paramName: 'descrParam' }];
    const parameter = [{ name: 'descrParam', title: 'A descr Parameter', type: 'string' }];
    const wrapper = renderer.create(<ContentPackEntityConfig
      entity={entity}
      appliedParameter={appliedParameter}
      parameters={parameter}
    />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
