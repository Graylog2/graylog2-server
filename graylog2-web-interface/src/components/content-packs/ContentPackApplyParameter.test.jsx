import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';
import Entity from 'logic/content-packs/Entity';

import ContentPackApplyParameter from 'components/content-packs/ContentPackApplyParameter';

describe('<ContentPackApplyParameter />', () => {
  const entity = Entity.builder()
    .id('111-beef')
    .v('1.0')
    .data({
      name: { '@type': 'string', '@value': 'Input' },
      title: { '@type': 'string', '@value': 'A good input' },
      configuration: {
        listen_address: { '@type': 'string', '@value': '1.2.3.4' },
        port: { '@type': 'integer', '@value': '23' },
      },
    })
    .build();

  const parameter = { title: 'Port', name: 'PORT', type: 'integer', default_value: '23' };
  const appliedParameter = { configKey: 'configuration.port', paramName: parameter.name };
  const appliedParameterReadOnly = { configKey: 'configuration.port', paramName: parameter.name, readOnly: true };

  it('should render with full props', () => {
    const wrapper = mount(<ContentPackApplyParameter entity={entity}
                                                     parameters={[parameter]}
                                                     appliedParameter={[appliedParameter]} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with readOnly', () => {
    const wrapper = mount(<ContentPackApplyParameter entity={entity}
                                                     parameters={[parameter]}
                                                     appliedParameter={[appliedParameterReadOnly]} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with minimal props', () => {
    const wrapper = mount(<ContentPackApplyParameter entity={entity} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should apply a parameter', () => {
    const applyFn = jest.fn((configKey, paramName) => {
      expect(configKey).toEqual('configuration.port');
      expect(paramName).toEqual('PORT');
    });

    const wrapper = mount(<ContentPackApplyParameter entity={entity}
                                                     parameters={[parameter]}
                                                     appliedParameter={[]}
                                                     onParameterApply={applyFn} />);

    wrapper.find('select#config_key').simulate('change', { target: { name: 'config_key', value: 'configuration.port' } });
    wrapper.find('select#parameter').simulate('change', { target: { name: 'parameter', value: 'PORT' } });
    wrapper.find('form').simulate('submit');
    expect(applyFn.mock.calls.length).toBe(1);
  });

  it('should apply a parameter only once', () => {
    const applyFn = jest.fn((configKey, paramName) => {
      expect(configKey).toEqual('configuration.port');
      expect(paramName).toEqual('PORT');
    });

    const wrapper = mount(<ContentPackApplyParameter entity={entity}
                                                     parameters={[parameter]}
                                                     appliedParameter={[{ configKey: 'configuration.port', paramName: 'PORT' }]}
                                                     onParameterApply={applyFn} />);

    wrapper.find('select#config_key').simulate('change', { target: { name: 'config_key', value: 'configuration.port' } });
    wrapper.find('select#parameter').simulate('change', { target: { name: 'parameter', value: 'PORT' } });
    wrapper.find('form').simulate('submit');
    expect(applyFn.mock.calls.length).toBe(0);
  });

  it('should clear a parameter', () => {
    const clearFn = jest.fn((configKey) => {
      expect(configKey).toEqual('configuration.port');
    });

    const wrapper = mount(<ContentPackApplyParameter entity={entity}
                                                     parameters={[parameter]}
                                                     appliedParameter={[appliedParameter]}
                                                     onParameterClear={clearFn} />);

    wrapper.find('button[children="Clear"]').simulate('click');
    expect(clearFn.mock.calls.length).toBe(1);
  });
});
