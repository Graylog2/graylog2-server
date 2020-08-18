import React from 'react';
import { mount } from 'wrappedEnzyme';
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
    const wrapper = mount(<DataAdapterCreate saved={callback} types={types} />);

    expect(wrapper).toExist();
  });

  it('should render for types with defined frontend components', () => {
    const callback = () => {};
    const types = {
      someType: {
        type: 'someType',
      },
    };
    const wrapper = mount(<DataAdapterCreate saved={callback} types={types} />);

    expect(wrapper).toExist();
  });

  describe('with mocked console.error', () => {
    // eslint-disable-next-line no-console
    const consoleError = console.error;

    beforeAll(() => {
      // eslint-disable-next-line no-console
      console.error = jest.fn();
    });

    afterAll(() => {
      // eslint-disable-next-line no-console
      console.error = consoleError;
    });

    it('should render for types without defined frontend components', () => {
      const callback = () => {};
      const types = {
        unknownType: {
          type: 'unknownType',
        },
      };
      const wrapper = mount(<DataAdapterCreate saved={callback} types={types} />);

      expect(wrapper).toExist();
      // eslint-disable-next-line no-console
      expect(console.error.mock.calls.length).toBe(1);
    });
  });
});
