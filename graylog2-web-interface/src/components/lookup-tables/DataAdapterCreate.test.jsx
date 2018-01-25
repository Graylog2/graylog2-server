import React from 'react';
import renderer from 'react-test-renderer';

import mockComponent from 'helpers/mocking/MockComponent';

import DataAdapterCreate from './DataAdapterCreate';

jest.mock('components/common', () => ({
  Select: mockComponent('SelectMock'),
}));

jest.mock('components/bootstrap', () => ({
  Input: mockComponent('InputMock'),
}));

jest.mock('components/lookup-tables', () => ({
  DataAdapterForm: mockComponent('DataAdapterFormMock'),
}));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => ([
      {
        type: 'someType',
        displayName: 'Some Mocked Data Adapter Type',
      },
    ]),
  },
}));

describe('<DataAdapterCreate />', () => {
  it('should render with empty parameters', () => {
    const callback = () => {};
    const types = {};
    const wrapper = renderer.create(<DataAdapterCreate saved={callback} types={types} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render for types with defined frontend components', () => {
    const callback = () => {};
    const types = {
      someType: {
        type: 'someType',
      },
    };
    const wrapper = renderer.create(<DataAdapterCreate saved={callback} types={types} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render for types without defined frontend components', () => {
    const callback = () => {};
    const types = {
      unknownType: {
        type: 'unknownType',
      },
    };
    const wrapper = renderer.create(<DataAdapterCreate saved={callback} types={types} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
